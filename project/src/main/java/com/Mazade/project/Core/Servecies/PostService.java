package com.Mazade.project.Core.Servecies;

import com.Mazade.project.Common.DTOs.PaginationDTO;
import com.Mazade.project.Common.Entities.Auction;
import com.Mazade.project.Common.Entities.AuctionBidTracker;
import com.Mazade.project.Common.Entities.Post;
import com.Mazade.project.Common.Enums.AuctionStatus;
import com.Mazade.project.Common.Enums.Category;
import com.Mazade.project.Common.Enums.Status;
import com.Mazade.project.Core.Repsitories.AuctionBidTrackerRepository;
import com.Mazade.project.Core.Repsitories.PostRepository;
import com.Mazade.project.Core.Repsitories.UserRepository;
import com.Mazade.project.WebApi.Exceptions.UserNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.Mazade.project.WebApi.Config.JwtService.log;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final CloudinaryService cloudinaryService;
    private final AuctionService auctionService ;
    private final UserRepository userRepository;
    private final AuctionBidTrackerRepository auctionBidTrackerRepository;

    @Autowired
    public PostService(PostRepository postRepository, CloudinaryService cloudinaryService,
                       AuctionService auctionService, UserRepository userRepository,
                       AuctionBidTrackerRepository auctionBidTrackerRepository) {
        this.postRepository = postRepository;
        this.cloudinaryService = cloudinaryService;
        this.auctionService = auctionService;
        this.userRepository = userRepository;
        this.auctionBidTrackerRepository = auctionBidTrackerRepository;
    }



    @Transactional
    public Post addPost(Post post, List<MultipartFile> images) throws IOException {
        if (images == null || images.isEmpty()) {
            throw new IllegalArgumentException("At least one image is required");
        }

        // Upload multiple images to Cloudinary
        List<String> imageUrls = cloudinaryService.uploadMultipleImages(images, "posts");
        String mediaString = String.join(",", imageUrls);
        post.setMedia(mediaString);

        // Find or create an appropriate auction for this post
        Category category = post.getCategory();
        Auction auction = auctionService.findOrCreateAuctionForCategory(category);
        post.setAuction(auction);

        // Add the post to the auction and update its status
        auctionService.addPostToAuction(post, auction);

        // Save and return the post
        return postRepository.save(post);
    }

    @Transactional
    public Post updatePostStatus(Long postId, Status status) throws UserNotFoundException {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new UserNotFoundException("Post not found"));

        // Set the new status
        post.setStatus(status);
        Post savedPost = postRepository.save(post);

        // If the status is set to COMPLETED, check if all posts in this auction are completed
        if (status == Status.COMPLETED) {
            auctionService.checkAndHandleAuctionCompletion(post.getAuction());
        }

        return savedPost;
    }
    @Transactional
    public Post accepetPost(Long postId) throws UserNotFoundException {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new UserNotFoundException("Post not found"));

        post.setAccepted(true);
        return postRepository.save(post);
    }
    @Transactional
    public PaginationDTO<Post> getAllPost(int page, int size, String search, Category category,
                                          Boolean sortByDate, Boolean sortByPrice, Boolean sortByRating) {
        if (page < 1) {
            page = 1;
        }

        Pageable pageable = PageRequest.of(page - 1, size);
        if (search != null && search.isEmpty()) {
            search = null;
        }

        Page<Post> postsPage;

        if (sortByDate) {
        postsPage = postRepository.SortByDate(pageable,search, category);
        } else if (sortByPrice) {
            postsPage = postRepository.SortByPrice(pageable,search, category);

        } else if (sortByRating) {
            postsPage = postRepository.SortByRating(pageable,search, category);
        } else {
            postsPage = postRepository.findAllPosts(pageable,search, category);
        }
        PaginationDTO<Post> paginationDTO = new PaginationDTO<>();
        paginationDTO.setTotalElements(postsPage.getTotalElements());
        paginationDTO.setTotalPages(postsPage.getTotalPages());
        paginationDTO.setSize(postsPage.getSize());
        paginationDTO.setNumber(postsPage.getNumber() + 1);
        paginationDTO.setNumberOfElements(postsPage.getNumberOfElements());
        paginationDTO.setContent(postsPage.getContent());
        return paginationDTO;
    }

    @Transactional
    public Post getPostById(Long postId) throws UserNotFoundException {
        Post post = postRepository.findByIdAndAccepted(postId)
                .orElseThrow(() -> new UserNotFoundException("Post not found with id: " + postId));

        post.setViewCount(post.getViewCount() + 1);
        postRepository.save(post);

        return post;
    }

    @Transactional
    public Post increasePostFinalPrice(Long postId, double amount) throws UserNotFoundException {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new UserNotFoundException("Post not found with id: " + postId));

        // Check if the auction status is IN_PROGRESS
        if (post.getAuction().getStatus() != AuctionStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("Cannot place bid: auction is not in progress");
        }

        // --- REVISED BID VALIDATION LOGIC ---

        // Case 1: The auction has NO bids yet.
        if (post.getFinalPrice() == 0.0) {
            // For the first bid, the amount must be at least the starting price.
            if (amount < post.getStartPrice()) {

                throw new IllegalArgumentException(
                        String.format("First bid of %.2f NIS is below the starting price of %.2f NIS", amount, post.getStartPrice())
                );
            }
            // If amount >= post.getStartPrice(), the bid is valid.
            System.out.println("Starting new auction with the first bid: " + amount);

        } else {
            // Case 2: The auction already has bids.
            // The new bid must be greater than the current price + the bid step.
            double currentPrice = post.getFinalPrice();
            double minimumBid = currentPrice + post.getBidStep();

            if (amount < minimumBid) {
                log.info(" 💰hhhhhhhhhhhhhh Placing bid for post {}: current price = {}, bid amount = {}",
                        postId, post.getFinalPrice(), amount);
                throw new IllegalArgumentException(
                        String.format("Bid amount %.2f is below minimum required: %.2f NIS", amount, minimumBid)
                );
            }
        }

        // --- END OF REVISED LOGIC ---

        // Set the new highest bid
        double previousPrice = post.getFinalPrice() != 0.0 ? post.getFinalPrice() : post.getStartPrice();
        post.setFinalPrice(amount);

        log.info("💰 Valid bid placed for post {}. Price updated from {} to {}",
                postId, previousPrice, amount);

        return postRepository.save(post);
    }

    @Transactional
    public PaginationDTO<Post> getPostsByUserId(Long userId, int page, int size, Category category) throws UserNotFoundException {
        if (page < 1) {
            page = 1;
        }
        // Check if the user exists
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException("User not found with id: " + userId);
        }

        Pageable pageable = PageRequest.of(page - 1, size);

        // Find the posts for the specified user with optional category filter
        Page<Post> postsPage = postRepository.findByUserIdAndCategoryOrderByCreatedDateDesc(userId, category, pageable);

        // Convert to PaginationDTO
        PaginationDTO<Post> paginationDTO = new PaginationDTO<>();
        paginationDTO.setTotalElements(postsPage.getTotalElements());
        paginationDTO.setTotalPages(postsPage.getTotalPages());
        paginationDTO.setSize(postsPage.getSize());
        paginationDTO.setNumber(postsPage.getNumber() + 1);
        paginationDTO.setNumberOfElements(postsPage.getNumberOfElements());
        paginationDTO.setContent(postsPage.getContent());

        return paginationDTO;
    }

    @Transactional
    public PaginationDTO<Post> getPostsWaitingForApproval(int page, int size) {
        if (page < 1) {
            page = 1;
        }

        Pageable pageable = PageRequest.of(page - 1, size);
        // Using a JPA Specification to find posts where isAccepted is false
        Page<Post> postsPage = postRepository.findAllToAccept(pageable);

        PaginationDTO<Post> paginationDTO = new PaginationDTO<>();
        paginationDTO.setTotalElements(postsPage.getTotalElements());
        paginationDTO.setTotalPages(postsPage.getTotalPages());
        paginationDTO.setSize(postsPage.getSize());
        paginationDTO.setNumber(postsPage.getNumber() + 1);
        paginationDTO.setNumberOfElements(postsPage.getNumberOfElements());
        paginationDTO.setContent(postsPage.getContent());

        return paginationDTO;
    }

    @Transactional
    public PaginationDTO<Post> getWonPostsByUserId(Long userId, int page, int size, Category category) throws UserNotFoundException {
        userRepository.findById(userId).orElseThrow(
                () -> new UserNotFoundException("User not found with id: " + userId));

        if (page < 1) {
            page = 1;
        }

        Pageable pageable = PageRequest.of(page - 1, size);
        Page<Post> postsPage = postRepository.findAcceptedPostsByWinnerIdAndCategory(userId, category, pageable);

        PaginationDTO<Post> paginationDTO = new PaginationDTO<>();
        paginationDTO.setTotalElements(postsPage.getTotalElements());
        paginationDTO.setTotalPages(postsPage.getTotalPages());
        paginationDTO.setSize(postsPage.getSize());
        paginationDTO.setNumber(postsPage.getNumber() + 1);
        paginationDTO.setNumberOfElements(postsPage.getNumberOfElements());
        paginationDTO.setContent(postsPage.getContent());

        return paginationDTO;
    }

    @Transactional
    public Post deletePost(Long postId) throws UserNotFoundException {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new UserNotFoundException("Post not found with id: " + postId));

        // Set the post to inactive
        post.setAccepted(false);
        return postRepository.save(post);
    }

    @Transactional
    public PaginationDTO<Post> getWaitingPostsForActiveAuction(Long auctionId, int page, int size, Category category) throws UserNotFoundException {
        if (page < 1) {
            page = 1;
        }

        Pageable pageable = PageRequest.of(page - 1, size);

        // Check if there are any IN_PROGRESS posts for this auction
        long inProgressCount = postRepository.countInProgressPostsForAuction(auctionId);

        // Determine which statuses to include
        List<Status> statusesToInclude;
        if (inProgressCount > 0) {
            // If there are IN_PROGRESS posts, include both WAITING and IN_PROGRESS
            statusesToInclude = Arrays.asList(Status.WAITING, Status.IN_PROGRESS);
        } else {
            // If no IN_PROGRESS posts, only include WAITING
            statusesToInclude = Arrays.asList(Status.WAITING);
        }

        // Find the posts for the specified auction with filters
        Page<Post> postsPage = postRepository.findPostsForActiveAuctionByStatuses(auctionId, statusesToInclude, category, pageable);

        // If this is the first page, has content, and no IN_PROGRESS posts exist yet, mark the oldest post as IN_PROGRESS
        if (page == 1 && postsPage.hasContent() && inProgressCount == 0) {
            Post oldestPost = postsPage.getContent().get(0);
            updatePostStatus(oldestPost.getId(), Status.IN_PROGRESS);

            // Now include both statuses for the refresh
            statusesToInclude = Arrays.asList(Status.WAITING, Status.IN_PROGRESS);

            // Refresh the page content after updating status
            postsPage = postRepository.findPostsForActiveAuctionByStatuses(auctionId, statusesToInclude, category, pageable);
        }

        // Convert to PaginationDTO
        PaginationDTO<Post> paginationDTO = new PaginationDTO<>();
        paginationDTO.setTotalElements(postsPage.getTotalElements());
        paginationDTO.setTotalPages(postsPage.getTotalPages());
        paginationDTO.setSize(postsPage.getSize());
        paginationDTO.setNumber(postsPage.getNumber() + 1);
        paginationDTO.setNumberOfElements(postsPage.getNumberOfElements());
        paginationDTO.setContent(postsPage.getContent());

        return paginationDTO;
    }

    @Transactional
    public void getHighestBidderId(Long postId) throws UserNotFoundException {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new UserNotFoundException("Post not found with id: " + postId));

        // Find the most recent bid tracker for this post
        Optional<AuctionBidTracker> latestBidder = auctionBidTrackerRepository.findTopByPostIdOrderByCreatedDateDesc(postId);

        if (latestBidder.isPresent()) {
            // Extract the user ID from the userIdentifier (format: "user-userId")
            String userIdentifier = latestBidder.get().getUserIdentifier();
            if (userIdentifier != null && userIdentifier.startsWith("user-")) {
                try {
                    Long userId = Long.parseLong(userIdentifier.substring(5)); // Extract the ID part

                    // Update the post with the winner ID
                    post.setWinnerId(userId);
                    postRepository.save(post);

                    log.info("🏆 Set winner ID {} for post ID {}", userId, postId);
                } catch (NumberFormatException e) {
                    log.error("Failed to parse user ID from userIdentifier: {}", userIdentifier, e);
                }
            }
        }
    }

}

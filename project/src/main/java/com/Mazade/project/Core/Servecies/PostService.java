package com.Mazade.project.Core.Servecies;

import com.Mazade.project.Common.DTOs.PaginationDTO;
import com.Mazade.project.Common.Entities.Auction;
import com.Mazade.project.Common.Entities.Post;
import com.Mazade.project.Common.Entities.User;
import com.Mazade.project.Common.Enums.AuctionStatus;
import com.Mazade.project.Common.Enums.Category;
import com.Mazade.project.Common.Enums.Status;
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
import java.util.List;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final CloudinaryService cloudinaryService;
    private final AuctionService auctionService ;
    private final UserRepository userRepository;

    @Autowired
    public PostService(PostRepository postRepository, CloudinaryService cloudinaryService,
                       AuctionService auctionService, UserRepository userRepository){
        this.postRepository = postRepository;
        this.cloudinaryService = cloudinaryService;
        this.auctionService = auctionService;
        this.userRepository = userRepository;
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
            throw new IllegalArgumentException("Cannot increase final price: auction is not in progress");
        }
            // check if the post bid step is less than the amount
        if (amount < post.getBidStep()) {
            throw new IllegalArgumentException("Amount must be greater than or equal to the bid step");
        }
        // If final price is 0 (initialized but not set), use startPrice as base
        if (post.getFinalPrice() == 0) {
            post.setFinalPrice(post.getStartPrice() + amount);
        } else {
            // Otherwise add to existing final price
            post.setFinalPrice(post.getFinalPrice() + amount);
        }

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
}

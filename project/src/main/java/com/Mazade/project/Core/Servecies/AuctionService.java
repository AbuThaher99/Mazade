package com.Mazade.project.Core.Servecies;

import com.Mazade.project.Common.DTOs.AuctionDTO;
import com.Mazade.project.Common.DTOs.PaginationDTO;
import com.Mazade.project.Common.Entities.Auction;
import com.Mazade.project.Common.Entities.Post;
import com.Mazade.project.Common.Enums.AuctionStatus;
import com.Mazade.project.Common.Enums.Category;
import com.Mazade.project.Common.Enums.Status;
import com.Mazade.project.Core.Repsitories.AuctionRepository;
import com.Mazade.project.Core.Repsitories.PostRepository;
import com.Mazade.project.WebApi.Exceptions.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuctionService {

    private final AuctionRepository auctionRepository;
    private final PostRepository postRepository;


    @Transactional
    public Auction findOrCreateAuctionForCategory(Category category) {
        // First try to find a WAITING auction for this category
        Auction waitingAuction = auctionRepository.findByCategoryAndStatus(category, AuctionStatus.WAITING);

        if (waitingAuction != null) {
            return waitingAuction;
        }

        // If no WAITING auction exists, create a new one
        return createNewAuctionForCategory(category);
    }


    @Transactional
    public void addPostToAuction(Post post, Auction auction) {
        // Increment the post count
        int currentCount = auction.getPostCount() + 1;
        auction.setPostCount(currentCount);
        post.setAuctionPostNumber(currentCount);
        // Save the auction with the updated post count
        auctionRepository.save(auction);
    }

    @Transactional
    public void checkAndHandleAuctionCompletion(Auction auction) {
        if (auction.getStatus() == AuctionStatus.COMPLETED) {
            return;
        }

        List<Post> posts = postRepository.findByAuctionId(auction.getId());

        // If no posts, return
        if (posts.isEmpty()) {
            return;
        }

        // Check if all posts are completed
        boolean allCompleted = true;
        for (Post post : posts) {
            if (post.getStatus() != Status.COMPLETED) {
                allCompleted = false;
                break;
            }
        }

        if (allCompleted) {
            // Mark this auction as completed
            auction.setStatus(AuctionStatus.COMPLETED);
            auctionRepository.save(auction);

            // Check if a WAITING auction for this category already exists
            Auction existingWaitingAuction = auctionRepository.findByCategoryAndStatus(
                    auction.getCategory(), AuctionStatus.WAITING);

            // Only create a new auction if there isn't already a WAITING one
            if (existingWaitingAuction == null) {
                createNewAuctionForCategory(auction.getCategory());
            }
        }
    }

    @Transactional
    public Auction createNewAuctionForCategory(Category category) {
        Auction newAuction = Auction.builder()
                .category(category)
                .postCount(0)
                .status(AuctionStatus.WAITING)
                .build();
        return auctionRepository.save(newAuction);
    }

    @Transactional
    public Auction updateAuctionStatus(Long auctionId, AuctionStatus newStatus) throws UserNotFoundException {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new UserNotFoundException("Auction not found"));

        auction.setStatus(newStatus);
        return auctionRepository.save(auction);
    }

    @Transactional
    public PaginationDTO<Auction> getAllAuctions(int page, int size, AuctionStatus status) {
        if (page < 1) {
            page = 1;
        }

        Pageable pageable = PageRequest.of(page - 1, size);
        Page<Auction> auctionsPage = auctionRepository.findAllWithFilters(pageable, status);

        PaginationDTO<Auction> paginationDTO = new PaginationDTO<>();
        paginationDTO.setTotalElements(auctionsPage.getTotalElements());
        paginationDTO.setTotalPages(auctionsPage.getTotalPages());
        paginationDTO.setSize(auctionsPage.getSize());
        paginationDTO.setNumber(auctionsPage.getNumber() + 1);
        paginationDTO.setNumberOfElements(auctionsPage.getNumberOfElements());
        paginationDTO.setContent(auctionsPage.getContent());

        return paginationDTO;
    }

    @Transactional
    public List<Auction> getAuctionsByStatus(AuctionStatus status) {
        return auctionRepository.findByStatus(status);
    }

    @Transactional
    public Auction getAuctionById(Long id) {
        return auctionRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("Auction not found"));
    }

    @Transactional
    public AuctionDTO getAuctionByCategoryAndStatus(Category category, AuctionStatus status) {
        Auction auction = auctionRepository.findByCategoryAndStatus(category, status);
        if (auction == null) {
            throw new UsernameNotFoundException("Auction not found for category: " + category + " and status: " + status);
        }
        return new AuctionDTO(
                auction.getId(),
                auction.getCategory().name(),
                auction.getStatus(),
                auction.getPostCount(),
                auction.getCreatedDate().toString()
        );

    }
}
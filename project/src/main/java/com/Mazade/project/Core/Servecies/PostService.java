package com.Mazade.project.Core.Servecies;

import com.Mazade.project.Common.DTOs.PaginationDTO;
import com.Mazade.project.Common.Entities.Post;
import com.Mazade.project.Common.Enums.Category;
import com.Mazade.project.Core.Repsitories.PostRepository;
import lombok.RequiredArgsConstructor;
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

    @Autowired
    public PostService(PostRepository postRepository, CloudinaryService cloudinaryService) {
        this.postRepository = postRepository;
        this.cloudinaryService = cloudinaryService;
    }



    @Transactional
    public Post addPost(Post post, List<MultipartFile> images) throws IOException {
        if (images == null || images.isEmpty()) {
            throw new IllegalArgumentException("At least one image is required");
        }

        // Upload multiple images to Cloudinary
        List<String> imageUrls = cloudinaryService.uploadMultipleImages(images, "posts");

        // Join the URLs with commas
        String mediaString = String.join(",", imageUrls);

        // Set the media field in the post
        post.setMedia(mediaString);

        // Save and return the post
        return postRepository.save(post);
    }

    @Transactional
    public PaginationDTO<Post> getAllPost(int page, int size, String search, String category,
                                          Boolean sortByDate, Boolean sortByPrice, Boolean sortByRating) {
        if (page < 1) {
            page = 1;
        }

        Pageable pageable = PageRequest.of(page - 1, size);
        if (search != null && search.isEmpty()) {
            search = null;
        }
        if (category != null && category.isEmpty()) {
            category = null;
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
}

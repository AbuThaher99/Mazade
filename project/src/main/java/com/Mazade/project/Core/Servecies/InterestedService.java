package com.Mazade.project.Core.Servecies;

import com.Mazade.project.Common.DTOs.PaginationDTO;
import com.Mazade.project.Common.Entities.Interested;
import com.Mazade.project.Common.Entities.Post;
import com.Mazade.project.Common.Entities.User;
import com.Mazade.project.Core.Repsitories.InterestedRepository;
import com.Mazade.project.Core.Repsitories.PostRepository;
import com.Mazade.project.Core.Repsitories.UserRepository;
import com.Mazade.project.WebApi.Exceptions.UserNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InterestedService {

    private final InterestedRepository interestedRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;

    @Autowired
    public InterestedService(InterestedRepository interestedRepository,
                             UserRepository userRepository,
                             PostRepository postRepository) {
        this.interestedRepository = interestedRepository;
        this.userRepository = userRepository;
        this.postRepository = postRepository;
    }

    @Transactional
    public Interested addInterested(Long userId, Long postId) throws UserNotFoundException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new UserNotFoundException("Post not found with id: " + postId));

        // Check if already interested
        if (interestedRepository.existsByUserIdAndPostId(userId, postId)) {
            throw new IllegalArgumentException("User is already interested in this post");
        }

        Interested interested = Interested.builder()
                .user(user)
                .post(post)
                .build();

        return interestedRepository.save(interested);
    }

    @Transactional
    public void removeInterested(Long userId, Long postId) throws UserNotFoundException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new UserNotFoundException("Post not found with id: " + postId));

        interestedRepository.deleteByUserIdAndPostId(userId, postId);
    }

    @Transactional(readOnly = true)
    public PaginationDTO<Post> getInterestedPostsByUserId(Long userId, int page, int size) throws UserNotFoundException {
        userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        if (page < 1) {
            page = 1;
        }

        Pageable pageable = PageRequest.of(page - 1, size);
        Page<Post> postsPage = interestedRepository.findPostsByUserIdPaginated(userId, pageable);

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
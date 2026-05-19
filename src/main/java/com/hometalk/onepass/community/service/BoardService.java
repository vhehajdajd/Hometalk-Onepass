package com.hometalk.onepass.community.service;

import com.hometalk.onepass.community.dto.response.BoardResponseDTO;
import com.hometalk.onepass.community.entity.Board;
import com.hometalk.onepass.community.exception.InvalidBoardCodeException;
import com.hometalk.onepass.community.repository.BoardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService {
    private final BoardRepository boardRepository;

    public List<BoardResponseDTO> findAll() {
        return boardRepository.findAll().stream()
                .map(BoardResponseDTO::from)
            .collect(Collectors.toList());
    }

    public BoardResponseDTO findById(Long id) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new InvalidBoardCodeException(String.valueOf(id)));
        return BoardResponseDTO.from(board);
    }

    public BoardResponseDTO findByCode(String code) {
        Board board = boardRepository.findByCode(code)
                .orElseThrow(() -> new InvalidBoardCodeException(code));
        return BoardResponseDTO.from(board);
    }
}

package com.hometalk.onepass.community.exception;

public class CategoryNotFoundException extends PostException {

    public CategoryNotFoundException(Long categoryId, String boardCode) {
        super("존재하지 않는 카테고리입니다. (Id: " + categoryId + ")", boardCode);
    }
}

package com.hometalk.onepass.community.exception;

public class CategoryNotFoundException extends PostException {

    public CategoryNotFoundException(Long categoryId, String boardCode) {
        super("해당 카테고리를 찾을 수 없습니다. id=" + categoryId, boardCode);
    }

    // 코드로 조회할 때 쓸 생성자 오버로딩
    public CategoryNotFoundException(String categoryCode) {
        super("해당 코드를 가진 카테고리가 없습니다. code=" + categoryCode, null);
    }
}

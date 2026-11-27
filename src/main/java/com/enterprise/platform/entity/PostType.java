package com.enterprise.platform.entity;

// 게시글 종류
public enum PostType {
    NOTICE, // 공지사항 - 관리자만 작성
    BOARD   // 자유게시판 - 로그인한 회원 누구나 작성
}
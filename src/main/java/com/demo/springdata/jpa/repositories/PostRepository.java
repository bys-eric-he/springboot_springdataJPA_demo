package com.demo.springdata.jpa.repositories;

import com.demo.springdata.jpa.models.Post;
import com.demo.springdata.jpa.models.QPost;

import com.querydsl.core.types.dsl.StringExpression;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends JpaRepository<Post, Long>, QuerydslPredicateExecutor<Post>, QuerydslBinderCustomizer<QPost> {
    default void customize(QuerydslBindings bindings, QPost post) {
        bindings.bind(post.title).first(StringExpression::containsIgnoreCase);
    }
}
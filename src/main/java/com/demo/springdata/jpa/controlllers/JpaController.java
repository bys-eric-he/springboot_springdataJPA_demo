package com.demo.springdata.jpa.controlllers;

import com.demo.springdata.jpa.models.Post;
import com.demo.springdata.jpa.models.QPost;
import com.demo.springdata.jpa.models.QUser;
import com.demo.springdata.jpa.models.User;
import com.demo.springdata.jpa.repositories.PostRepository;
import com.demo.springdata.jpa.repositories.UserRepository;
import com.google.common.collect.Lists;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.ComparableExpressionBase;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/*
import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;*/
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
public class JpaController {
    private final Logger logger = LoggerFactory.getLogger(JpaController.class);

    /*@PersistenceContext
    private EntityManager entityManager;*/

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private JPAQueryFactory jpaQueryFactory;

    @Autowired
    public JpaController(UserRepository userRepository, PostRepository postRepository, JPAQueryFactory jpaQueryFactory) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.jpaQueryFactory = jpaQueryFactory;
    }

    /*
    @PostConstruct
    public void init() {
        queryFactory = new JPAQueryFactory(entityManager);
    }*/

    @GetMapping("/users/emails")
    public Object userEmails() {
        QUser user = QUser.user;
        userRepository.findAll(user.name.eq("lufifcc"));
        userRepository.findAll(
                user.email.endsWith("@gmail.com")
                        .and(user.name.startsWith("lu"))
                        .and(user.id.in(Arrays.asList(520L, 1314L, 1L, 2L, 12L)))
        );

        userRepository.count(
                user.email.endsWith("@outlook.com")
                        .and(user.name.containsIgnoreCase("a"))
        );

        Iterable<User> rs = userRepository.findAll(
                user.email.endsWith("@qq.com")
                        .and(user.posts.size().goe(5))
        );

        List<User> users = Lists.newArrayList(rs);
        for (User u : users) {
            logger.info("user->name:{}->address:{}->email:{}", u.getName(), u.getAddress(), u.getEmail());
        }

        /*
        由于MySQL不支持Max函数,所以这里无法使用表达式 JPAExpressions.select(user.posts.size().max()).from(user)
        java.sql.SQLException: Invalid use of group function
        SELECT
            user0_.id AS id1_4_,
            user0_.address AS address2_4_,
            user0_.email AS email3_4_,
            user0_.NAME AS name4_4_
        FROM
            USER user0_
        WHERE
            ( SELECT count( posts1_.user_id ) FROM post posts1_ WHERE user0_.id = posts1_.user_id ) >= ( SELECT max( count( posts3_.user_id ) ) FROM USER user2_ CROSS JOIN post posts3_ WHERE user2_.id = posts3_.user_id )

        > 1111 - Invalid use of group function
        userRepository.findAll(
                user.posts.size().goe(JPAExpressions.select(user.posts.size().max()).from(user))
        );*/


        /*
        由于MySQL不支持Max函数,所以这里无法使用表达式 JPAExpressions.select(user.posts.size().max()).from(user)
        List<User> userList = jpaQueryFactory.selectFrom(user).where(
                user.posts.size().goe(JPAExpressions.select(user.posts.size().max()).from(user))
        ).fetch();

        for (User u : userList) {
            logger.info("user->name:{}->address:{}->email:{}", u.getName(), u.getAddress(), u.getEmail());
        }*/

        return jpaQueryFactory.selectFrom(user)
                .select(user.email)
                .fetch();
    }

    @GetMapping("users/names-emails")
    public Object userNamesEmails() {
        QUser user = QUser.user;
        return jpaQueryFactory.selectFrom(user)
                .select(user.email, user.name)
                .fetch()
                .stream()
                .map(tuple -> {
                    Map<String, String> map = new LinkedHashMap<>();
                    map.put("name", tuple.get(user.name));
                    map.put("email", tuple.get(user.email));
                    return map;
                }).collect(Collectors.toList());
    }


    @GetMapping("users")
    public Object users(@QuerydslPredicate(root = User.class) Predicate predicate) {
        return userRepository.findAll(predicate);
    }

    @GetMapping("users/{user}/posts")
    public Object posts(@PathVariable User user) {
        return user.getPosts();
    }

    @GetMapping("users/posts-count")
    public Object postCount() {
        QUser user = QUser.user;
        QPost post = QPost.post;
        return jpaQueryFactory.selectFrom(user)
                .leftJoin(user.posts, post)
                .select(user.id, user.name, post.count())
                .groupBy(user.id)
                .fetch()
                .stream()
                .map(tuple -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", tuple.get(user.id));
                    map.put("name", tuple.get(user.name));
                    map.put("posts_count", tuple.get(post.count()));
                    return map;
                }).collect(Collectors.toList());
    }

    @GetMapping("users/category-count")
    public Object postCategoryMax() {
        QUser user = QUser.user;
        QPost post = QPost.post;
        NumberExpression<Integer> java = post.category
                .name.lower().when("java").then(1).otherwise(0);
        NumberExpression<Integer> python = post.category
                .name.lower().when("python").then(1).otherwise(0);
        return jpaQueryFactory.selectFrom(user)
                .leftJoin(user.posts, post)
                .select(user.name, user.id, java.sum(), python.sum(), post.count())
                .groupBy(user.id)
                .orderBy(user.name.desc())
                .fetch()
                .stream()
                .map(tuple -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("username", tuple.get(user.name));
                    map.put("java_count", tuple.get(java.sum()));
                    map.put("python_count", tuple.get(python.sum()));
                    map.put("total_count", tuple.get(post.count()));
                    return map;
                }).collect(Collectors.toList());
    }

    @GetMapping("posts")
    public Object posts(@QuerydslPredicate(root = Post.class) Predicate predicate, Pageable pageable) {
        return postRepository.findAll(predicate, pageable);
    }

    @GetMapping("posts-summary")
    public Object postsSummary() {
        QPost post = QPost.post;
        ComparableExpressionBase<?> postTimePeriodsExp = post.createdAt.year();
        NumberExpression<Integer> java = post.category
                .name.lower().when("java").then(1).otherwise(0);
        NumberExpression<Integer> python = post.category
                .name.lower().when("python").then(1).otherwise(0);
        return jpaQueryFactory.selectFrom(post)
                .groupBy(postTimePeriodsExp)
                .select(postTimePeriodsExp, java.sum(), python.sum(), post.count())
                .orderBy(postTimePeriodsExp.asc())
                .fetch()
                .stream()
                .map(tuple -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("time_period", tuple.get(postTimePeriodsExp));
                    map.put("java_count", tuple.get(java.sum()));
                    map.put("python_count", tuple.get(python.sum()));
                    map.put("total_count", tuple.get(post.count()));
                    return map;
                }).collect(Collectors.toList());
    }
}
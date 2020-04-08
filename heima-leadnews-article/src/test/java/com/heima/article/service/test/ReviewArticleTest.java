package com.heima.article.service.test;


import com.heima.article.service.ReviewCrawlerArticleService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@RunWith(SpringRunner.class)
public class ReviewArticleTest {


    @Autowired
    private ReviewCrawlerArticleService reviewCrawlerArticleService;

    @Test
    public void testReviewCraeler() throws Exception {
        reviewCrawlerArticleService.autoReivewArticleByCrawler();
    }
}

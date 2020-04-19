package com.heima.article.service.test;

import com.heima.article.service.ApHotArticleService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @program: heima-leadnews
 * @description:
 * @author: RookieWZW
 * @create: 2020-04-10 22:05
 */
@SpringBootTest
@RunWith(SpringRunner.class)
public class HotArticleTest {

    @Autowired
    private ApHotArticleService apHotArticleService;

    @Test
    public void testcomputeHotArticle()throws Exception{
        apHotArticleService.computeHotArticle();
    }
}
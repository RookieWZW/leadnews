package com.heima.article.service.impl;

import com.alibaba.fastjson.JSON;
import com.heima.article.service.ApArticleSearchService;
import com.heima.article.util.HttpClientUtils;
import com.heima.common.common.contants.ESIndexConstants;
import com.heima.model.article.dtos.UserSearchDto;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.article.pojos.ApAssociateWords;
import com.heima.model.article.pojos.ApHotWords;
import com.heima.model.behavior.pojos.ApBehaviorEntry;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.mappers.app.*;
import com.heima.model.user.pojos.ApUser;
import com.heima.model.user.pojos.ApUserSearch;
import com.heima.utils.threadlocal.AppThreadLocalUtils;
import io.searchbox.action.Action;
import io.searchbox.client.JestClient;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.net.nntp.Article;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@SuppressWarnings("all")
public class ApArticleSearchServiceImpl implements ApArticleSearchService {


    @Autowired
    private ApBehaviorEntryMapper apBehaviorEntryMapper;

    public ResponseResult getEntryId(UserSearchDto dto){
        ApUser user = AppThreadLocalUtils.getUser();
        // 用户和设备不能同时为空
        if(user == null && dto.getEquipmentId()==null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_REQUIRE);
        }
        Long userId = null;
        if(user!=null){
            userId = user.getId();
        }
        ApBehaviorEntry apBehaviorEntry = apBehaviorEntryMapper.selectByUserIdOrEquipemntId(userId, dto.getEquipmentId());
        // 行为实体找以及注册了，逻辑上这里是必定有值得，除非参数错误
        if(apBehaviorEntry==null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        return ResponseResult.okResult(apBehaviorEntry.getId());
    }

    @Autowired
    private ApUserSearchMapper apUserSearchMapper;

    @Override
    public ResponseResult findUserSearch(UserSearchDto dto) {
        if(dto.getPageSize() > 50 || dto.getPageSize() < 0){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //先获取行为实体id
        ResponseResult result = getEntryId(dto);
        if(result.getCode()!=AppHttpCodeEnum.SUCCESS.getCode()){
            return result;
        }
        //查询搜索记录
        List<ApUserSearch> list = apUserSearchMapper.selectByEntryId((int)result.getData(), 5);
        return ResponseResult.okResult(list);
    }

    @Override
    public ResponseResult delUserSearch(UserSearchDto dto) {
        if(dto.getHisList()==null || dto.getHisList().size() <= 0){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_REQUIRE);
        }
        //获取行为实体id
        ResponseResult result = getEntryId(dto);
        if(result.getCode()!=AppHttpCodeEnum.SUCCESS.getCode()){
            return result;
        }
        List<Integer> ids = dto.getHisList().stream().map(ApUserSearch::getId).collect(Collectors.toList());
        //通过id修改状态
        int count = apUserSearchMapper.delUserSearch((int)result.getData(), ids);
        return ResponseResult.okResult(count);
    }

    @Override
    public ResponseResult clearUserSearch(UserSearchDto dto) {
        ResponseResult result = getEntryId(dto);
        if(result.getCode()!=AppHttpCodeEnum.SUCCESS.getCode()){
            return result;
        }
        int count = apUserSearchMapper.clearUserSearch((int) result.getData());
        return ResponseResult.okResult(count);
    }

    @Autowired
    private ApHotWordsMapper apHotWordsMapper;

    @Override
    public ResponseResult hotkeywords(String date) {
        if(StringUtils.isEmpty(date)){
            date = DateFormatUtils.format(new Date(),"yyyy-MM-dd");
        }
        List<ApHotWords> apHotWords = apHotWordsMapper.queryByHotDate(date);
        return ResponseResult.okResult(apHotWords);
    }

    @Autowired
    private ApAssociateWordsMapper apAssociateWordsMapper;

    @Override
    public ResponseResult searchAssociate(UserSearchDto dto)  {
        if(dto.getPageSize() > 50 || dto.getPageSize()<1){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
       List<ApAssociateWords> apAssociateWords = apAssociateWordsMapper.selectByAssociateWords("%"+dto.getSearchWords()+"%", dto.getPageSize());

        if (apAssociateWords==null || apAssociateWords.size()==0){
            apAssociateWords = searchAssociateByBaiDu(dto);
        }
        if(apAssociateWords.size()>5){
            apAssociateWords = apAssociateWords.subList(0,5);
        }
        return ResponseResult.okResult(apAssociateWords);

    }
    private List<ApAssociateWords> searchAssociateByBaiDu(UserSearchDto dto)  {
        String jsonData = null;
        String url = "http://suggestion.baidu.com/su?wd="+dto.getSearchWords()+"&p=3&cb=window.bdsug.sug#";
        try {
            jsonData = HttpClientUtils.get(url, null,"UTF-8");
        } catch (Exception e) {
            return null;
        }
        String[] jsonDataList = jsonData.split("s:");
        jsonData =  jsonDataList[1].substring(0,jsonDataList[1].length()-3);
        List<String> list = (List) JSON.parse(jsonData);
        List<ApAssociateWords> apAssociateWordsList =  new ArrayList<>();
        for (String string:list) {
            ApAssociateWords apAssociateWords = new ApAssociateWords();
            apAssociateWords.setAssociateWords(string);
            apAssociateWords.setCreatedTime(new Date());
            apAssociateWordsMapper.insert(apAssociateWords);
            apAssociateWordsList.add(apAssociateWords);
        }
        return apAssociateWordsList;
    }



    @Override
    public ResponseResult saveUserSearch(Integer entryId, String searchWords) {
        //去检查当前数据是否存在
        int count = apUserSearchMapper.checkExist(entryId, searchWords);
        if(count>0){
            return ResponseResult.okResult(1);
        }
        //不存在，保存
        ApUserSearch apUserSearch = new ApUserSearch();
        apUserSearch.setEntryId(entryId);
        apUserSearch.setKeyword(searchWords);
        apUserSearch.setStatus(1);
        apUserSearch.setCreatedTime(new Date());
        int insert = apUserSearchMapper.insert(apUserSearch);
        return ResponseResult.okResult(insert);
    }

    @Autowired
    private JestClient jestClient;

    @Autowired
    private ApArticleMapper apArticleMapper;

    @Override
    public ResponseResult esArticleSearch(UserSearchDto dto) {
        //保存搜索记录,只在第一页查询的时候进行保存
        if(dto.getFromIndex()==0){
            ResponseResult ret = getEntryId(dto);
            if(ret.getCode()!=AppHttpCodeEnum.SUCCESS.getCode()){
                return ret;
            }
            saveUserSearch((int)ret.getData(),dto.getSearchWords());
        }
        //构建查询条件  jestClient
        //按照关键字查询，分页查询
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.matchQuery("title",dto.getSearchWords()));

        searchSourceBuilder.from(dto.getFromIndex());
        searchSourceBuilder.size(dto.getPageSize());
        Search search = new Search.Builder(searchSourceBuilder.toString())
                .addIndex(ESIndexConstants.ARTICLE_INDEX)
                .addType(ESIndexConstants.DEFAULT_DOC).build();
        try {
            SearchResult searchResult = jestClient.execute(search);
            List<ApArticle> apArticles = searchResult.getSourceAsObjectList(ApArticle.class);
            List<ApArticle> resultList = new ArrayList<>();
            for (ApArticle apArticle : apArticles) {
                apArticle = apArticleMapper.selectById(Long.valueOf(apArticle.getId()));
                if(apArticle==null){
                    continue;
                }
                resultList.add(apArticle);
            }
            //获取结果
            return ResponseResult.okResult(resultList);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
    }
}

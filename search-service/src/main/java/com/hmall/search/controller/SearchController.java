package com.hmall.search.controller;


import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmall.common.domain.PageDTO;
import com.hmall.common.exception.BadRequestException;
import com.hmall.common.utils.BeanUtils;
import com.hmall.search.domain.dto.ItemDTO;
import com.hmall.search.domain.dto.ItemDoc;
import com.hmall.search.domain.query.ItemPageQuery;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Api(tags = "搜索相关接口")
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {
    private final RestHighLevelClient client;



    @ApiOperation("搜索商品")
    @GetMapping("/list")
    public PageDTO<ItemDTO> search(ItemPageQuery query) {
        int pageNo = query.getPageNo();
        int pageSize = query.getPageSize();
        //1.创建request
        SearchRequest request = new SearchRequest("items");
        //2.设置参数
        //2.1搜索条件参数
        BoolQueryBuilder bool = QueryBuilders.boolQuery();
        //2.1.1关键字
        if (StrUtil.isNotEmpty(query.getKey())) {
            bool.must(QueryBuilders.matchQuery("name", query.getKey()));
        }
        //2.1.2 分类过滤
        if (StrUtil.isNotEmpty(query.getCategory())) {
            bool.filter(QueryBuilders.termQuery("category", query.getCategory()));
        }
        //2.1.3 品牌过滤
        if (StrUtil.isNotEmpty(query.getBrand())) {
            bool.filter(QueryBuilders.termQuery("brand", query.getBrand()));
        }
        //2.1.4 价格过滤
        if (query.getMinPrice() != null && query.getMaxPrice() != null) {
            bool.filter(QueryBuilders.rangeQuery("price").gte(query.getMinPrice()).lte(query.getMaxPrice()));
        }
        request.source().query(bool);
        //2.2分页参数
        request.source().from((pageNo - 1) * pageSize).size(pageSize);
        //2.3排序参数
        String sortBy = StrUtil.isNotEmpty(query.getSortBy()) ? query.getSortBy() : "_score";
        SortOrder sortOrder = Boolean.TRUE.equals(query.getIsAsc()) ? SortOrder.ASC : SortOrder.DESC;
        request.source().sort(sortBy, sortOrder);
        //3.发送请求
        try {
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);
            //4.解析响应
            SearchHits searchHits = response.getHits();
            long total = searchHits.getTotalHits().value;
            long pages = (total + pageSize - 1) / pageSize;
            List<ItemDoc> itemDocs = handleResponse(searchHits);
            List<ItemDTO> list = BeanUtils.copyList(itemDocs, ItemDTO.class, (origin, target) -> {
                if (StrUtil.isNotEmpty(origin.getId())) {
                    target.setId(Long.parseLong(origin.getId()));
                }
            });
            return new PageDTO<>(total, pages, list);
        } catch (IOException e) {
            throw new BadRequestException("搜索服务异常");
        }
    }
    
    private List<ItemDoc> handleResponse(SearchHits searchHits) {
        // 遍历结果数组
        SearchHit[] hits = searchHits.getHits();
        List<ItemDoc> list = new ArrayList<>();
        for (SearchHit hit : hits) {
            // 得到_source，也就是原始json文档
            String source = hit.getSourceAsString();
            // 反序列化
            ItemDoc item = JSONUtil.toBean(source, ItemDoc.class);
            list.add(item);
        }
        return list;
    }
    @ApiOperation("过滤条件聚合")
    @PostMapping("/filters")
    public Map<String, List<String>> itemsfilters(@RequestBody ItemPageQuery query) {
        SearchRequest request = new SearchRequest("items");
        //1.设置查询条件
        BoolQueryBuilder bool = QueryBuilders.boolQuery();
        if (StrUtil.isNotEmpty(query.getKey())) {
            bool.must(QueryBuilders.matchQuery("name", query.getKey()));
        }
        if (StrUtil.isNotEmpty(query.getCategory())) {
            bool.filter(QueryBuilders.termQuery("category", query.getCategory()));
        }
        request.source().query(bool);
        //2.不需要返回文档，只要聚合结果
        request.source().size(0);
        //3.设置聚合：品牌聚合作为分类聚合的子聚合
        request.source().aggregation(
                AggregationBuilders.terms("category").field("category").size(7)
                        .subAggregation(AggregationBuilders.terms("brand").field("brand").size(7))
        );
        try {
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);
            //4.解析聚合结果
            Terms categoryTerms = response.getAggregations().get("category");
            List<String> categories = new ArrayList<>();
            List<String> brands = new ArrayList<>();
            for (Terms.Bucket categoryBucket : categoryTerms.getBuckets()) {
                categories.add(categoryBucket.getKeyAsString());
                //4.1.从分类桶中获取品牌子聚合
                Terms brandTerms = categoryBucket.getAggregations().get("brand");
                for (Terms.Bucket brandBucket : brandTerms.getBuckets()) {
                    brands.add(brandBucket.getKeyAsString());
                }
            }
            return Map.of("category", categories, "brand", brands);
        } catch (IOException e) {
            throw new BadRequestException("搜索服务异常");
        }
    }
}

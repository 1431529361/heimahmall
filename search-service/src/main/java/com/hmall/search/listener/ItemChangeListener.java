package com.hmall.search.listener;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.hmall.api.client.ItemClient;
import com.hmall.api.dto.Item;
import com.hmall.search.domain.dto.ItemDoc;
import lombok.RequiredArgsConstructor;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class ItemChangeListener {
    private final ItemClient itemClient;
    private final RestHighLevelClient client;

    @RabbitListener(bindings = @QueueBinding(value = @Queue(name = "search.item.change.queue", durable = "true"),
            exchange = @Exchange(name = "search.direct"),
            key = "item.add"
    ))
    public void listenItemAdd(Long itemId) throws IOException {
        Item item = itemClient.queryItemyById(itemId);
        ItemDoc itemDoc = BeanUtil.copyProperties(item, ItemDoc.class);
        String doc = JSONUtil.toJsonStr(itemDoc);
        IndexRequest request = new IndexRequest("items").id(itemDoc.getId());
        request.source(doc, XContentType.JSON);
        client.index(request, RequestOptions.DEFAULT);
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue(name = "search.item.change.queue", durable = "true"),
            exchange = @Exchange(name = "search.direct"),
            key = "item.update"
    ))
    public void listenItemUpdate(Long itemId) throws IOException {
        Item item = itemClient.queryItemyById(itemId);
        ItemDoc itemDoc = BeanUtil.copyProperties(item, ItemDoc.class);
        String doc = JSONUtil.toJsonStr(itemDoc);
        IndexRequest request = new IndexRequest("items").id(itemDoc.getId());
        request.source(doc, XContentType.JSON);
        client.index(request, RequestOptions.DEFAULT);
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue(name = "search.item.change.queue", durable = "true"),
            exchange = @Exchange(name = "search.direct"),
            key = "item.delete"
    ))
    public void listenItemDelete(Long itemId) throws IOException {
        DeleteRequest request = new DeleteRequest("items", String.valueOf(itemId));
        client.delete(request, RequestOptions.DEFAULT);
    }
}

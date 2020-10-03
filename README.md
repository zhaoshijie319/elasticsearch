# 一、简介

Elasticsearch：是一个分布式、可扩展、近实时的搜索与数据分析引擎

# 二、说明

## 应用

- 结构化搜索
- 数据分析
- 复杂的人类语言处理
- 地理位置和对象间关联关系

## 高可用

- 至少三个主节点
- 每个分片至少两个节点
- 每个分片至少有两个副本（一个主副本和一个或多个副本）
    
## 连接查询过程

- query
    - 1、协调节点广播每一个shard、命中文档的TopN优先级队列
    - 2、每个shard返回doc_id和_source给协调节点
    - 3、协调节点全局排序
- fetch
    - 4、协调节点发送批量的mutil get请求到相关shard
    - 5、加载shard返回协调节点

## 数据写入过程

- 1、协调节点通过hash找到shard，将写请求发送到对应节点上
- 2、对应节点对索引数据进行校验，然后写入到shard中
    - 数据写入到内存buffer和translog buffer（预写日志）
    - 每隔1s数据从buffer中refresh到系统缓存中，生成segment文件
    - 清空memory buffer
    - 每隔5s，translog 从buffer flush到磁盘中
    - 定期/定量从系统缓存中,结合translog内容flush index到磁盘中
- 3、主节点数据写入成功后，将数据并行发送到副本集节点
- 4、副本集节点写入数据成功后，发送ack信号给主节点
- 5、主节点发送ack给协调节点
- 6、协调节点发送ack给客户端

## 优化

- 建议
    - 不要返回大结果集
    - 避免大文件

- 查询
    - 精确搜索和词干混合
    - 多次查询结果评分受各分片影响不一致

- 索引速度
    - 使用批量请求
    - 多线程发送数据
    - 取消设置或增大索引间隔
    - 初始加载时禁用副本
    - 禁用换出java进程
    - 内存分配文件系统缓存至少一份
    - 使用默认id
    - 索引缓冲区大小至少占10%
 
- 调整搜索速度
    - 文档建模
    - 限制返回字段
    - 预索引字段
    - 无需范围查询将字段设为keyword
    - 避免脚本
    - 日期查询摄入
    - 强制合并只读索引
    - 预热搜索字段和文件系统缓存
    - 合理设置副本数

- 磁盘使用率
    - 禁用不需要的功能
    - 不适用默认的动态字符映射
    - 使用编解码器压缩
    - 使用足够小的数据类型
    
- 避免过度分片

# 三、使用

## 命令和请求

### 服务管理

- 查看集群状态
```http request
http://localhost:9200/_cat?v
```
- 查看所有索引
```http request
http://localhost:9200/_cat/indices?v
```

### 索引管理

- 查看所有的索引状态

    GET /_stats

- 打开/关闭索引

    POST /my_index/_close
    POST /my_index/_open

- 设置索引的读写

    PUT /my_source_index/_settings
```json5
{
  "settings": {
    "index.blocks.write": true
  }
}
```
```properties
index.blocks.read_only：设为true,则索引以及索引的元数据只可读
index.blocks.read_only_allow_delete：设为true，只读时允许删除。
index.blocks.read：设为true，则不可读。
index.blocks.write：设为true，则不可写。
index.blocks.metadata：设为true，则索引元数据不可读写。
```

- 添加索引

    PUT indexname
```json5
{
    "settings" : {
        "index" : {
            "number_of_shards" : 3,
            "number_of_replicas" : 2
        }
    },
    "mappings" : {
         "type1" : {
             "properties" : {
                 "field1" : { "type" : "text" }
             }
         }
     },
    "aliases" : {
         "alias_1" : {},
         "alias_2" : {
             "filter" : {
                 "term" : {"user" : "kimchy" }
             },
             "routing" : "kimchy"
         }
     }
}
```

- 查询索引

    GET /indexname

- 删除索引

    DELETE indexname

- 是否存在索引

    HEAD indexname

- 添加索引

    PUT indexname

- 修改索引备份数

    PUT /indexname/_settings
```json5
{
    "index" : {
        "number_of_replicas" : 2
    }
}
```

### 请求处理

- 请求格式
````http request
curl -X<VERB> '<PROTOCOL>://<HOST>:<PORT>/<PATH>?<QUERY_STRING>' -d '<BODY>'
````

- 复杂查询
```json5
{
  "query": {
    "bool": {
      "must": [
        {
          "match": {
            "age": "40"
          }
        }
      ],
      "must_not": [
        {
          "match": {
            "state": "ID"
          }
        }
      ],
      "filter": {
        "range": {
          "balance": {
            "gte": 20000,
            "lte": 30000
          }
        }
      }
    }
  },
  "from": 0,
  "size": 0,
  "sort": [
    {
      "name": "asc"
    }
  ],
  "highlight": {
    "fields": {
      "name": {
        "type": "plain"
      }
    }
  },
  "_source": {
    "includes": [
      "name"
    ],
    "excludes": [
      "love"
    ]
  },
  "aggs": {
    "group_by_state": {
      "terms": {
        "field": "state.keyword",
        "order": {
          "average_balance": "desc"
        }
      },
      "aggs": {
        "average_balance": {
          "avg": {
            "field": "balance"
          }
        }
      }
    }
  }
}
```
## Docker快速部署

```shell script
docker pull elasticsearch:7.3.2
docker run -p 9200:9200 -p 9300:9300 -e "discovery.type=single-node" docker.elastic.co/elasticsearch/elasticsearch:7.3.2
```

## JAVA

### 1、新建一个Spring Initializr项目

### 2、添加elasticsearch依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
</dependency>
```

### 3、修改配置文件

```properties
spring.elasticsearch.rest.uris=http://localhost:9200
```

### 4、创建Document

```java
@Document(indexName = "user")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class UserDocument {
    @Id
    private long id;
    @Field(type = FieldType.Text)
    private String name;
    @Field(type = FieldType.Integer)
    private Integer age;
    @Field(type = FieldType.Keyword)
    private String love;
}
```

### 5、创建Repository

```java
@Repository
public interface UserRepository extends ElasticsearchRepository<UserDocument, Long> {
    List<UserDocument> findByAgeAndLove(Integer age, String love);
}

```

### 6、编写测试代码

```java
@SpringBootTest
class ElasticsearchApplicationTests {
    @Resource
    UserRepository userRepository;
    @Resource
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    @Test
    public void testCreate(){
        elasticsearchRestTemplate.createIndex(UserDocument.class);
    }

    @Test
    public void testSave(){
        LinkedList<UserDocument> list = new LinkedList<>();
        list.add(new UserDocument(1L,"大怪兽1",1,"1"));
        list.add(new UserDocument(2L,"大怪兽2",2,"2"));
        list.add(new UserDocument(3L,"大怪兽3",3,"3"));
        list.add(new UserDocument(4L,"大怪兽4",4,"4"));
        list.add(new UserDocument(5L,"大怪兽5",5,"5"));
        userRepository.saveAll(list);
    }

    @Test
    public void testFind(){
        for (UserDocument userDocument : userRepository.findByAgeAndLove(3, "3")) {
            System.out.println(userDocument);
        }
    }

    @Test
    public void testSrarch() {
        SearchQuery searchQuery = new NativeSearchQueryBuilder()//构建查询对象
                .withQuery(QueryBuilders.matchQuery("name","仙女"))
                .withSourceFilter(new FetchSourceFilter(null,new String[]{"love"}))
                .withSort(SortBuilders.fieldSort("age").order(SortOrder.DESC))
                .withPageable(PageRequest.of(0, 2))
                .build();
        Page<UserDocument> userDocumentPage = userRepository.search(searchQuery);
        System.out.println("总数：" + userDocumentPage.getTotalElements());
        System.out.println("总页数：" + userDocumentPage.getTotalPages());
        for (UserDocument userDocument : userDocumentPage) {
            System.out.println(userDocument);
        }
    }

    @Test
    public void testAggs(){
        String aggName = "ageAggs";
        NativeSearchQuery nativeSearchQuery = new NativeSearchQueryBuilder()
                .addAggregation(AggregationBuilders.terms(aggName).field("love"))
                .build();
        AggregatedPage<UserDocument> list = elasticsearchRestTemplate.queryForPage(nativeSearchQuery, UserDocument.class);
        ParsedTerms terms = list.getAggregations().get(aggName);
        for (Terms.Bucket bucket : terms.getBuckets()) {
            System.out.println("key = " + bucket.getKey());
            System.out.println("docCount = " + bucket.getDocCount());
        }
    }

    @Test
    public void testHighlight() {
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.matchQuery("name", "仙女"))
                .withPageable(PageRequest.of(0, 4))
                .withHighlightFields(new HighlightBuilder.Field("name").preTags(new String[]{"<em>"}).postTags(new String[]{"/<em>"}))
                .build();
        SearchResultMapper resultMapper = new SearchResultMapper() {
            @Override
            public <T> AggregatedPage<T> mapResults(SearchResponse searchResponse, Class<T> aClass, Pageable pageable) {
                if (searchResponse.getHits().getTotalHits() <= 0) {
                    return null;
                }
                List<UserDocument> result = new ArrayList<>();
                for (SearchHit hit : searchResponse.getHits()) {
                    UserDocument userDocument = new UserDocument();
                    HighlightField itemTitle = hit.getHighlightFields().get("name");
                    if (itemTitle != null) {
                        userDocument.setName(itemTitle.fragments()[0].toString());
                    }
                    userDocument.setAge((Integer) hit.getSourceAsMap().get("age"));
                    userDocument.setLove((String) hit.getSourceAsMap().get("love"));
                    result.add(userDocument);
                }
                if (result.size() > 0) {
                    return new AggregatedPageImpl<>((List<T>) result);
                }
                return null;
            }
            @Override
            public <T> T mapSearchHit(SearchHit searchHit, Class<T> aClass) {
                return null;
            }
        };
        elasticsearchRestTemplate.queryForPage(searchQuery, UserDocument.class, resultMapper)
                .get().forEach(System.out::println);
    }
}
```

# 四、链接

[Elasticsearch官网](https://www.elastic.co/cn/ "Elasticsearch官网")

[spring-data-elasticsearch官网](https://spring.io/projects/spring-data-elasticsearch "spring-data-elasticsearch官网")

[ES请求列表](https://www.elastic.co/guide/en/elasticsearch/reference/current/rest-apis.html "ES请求列表")
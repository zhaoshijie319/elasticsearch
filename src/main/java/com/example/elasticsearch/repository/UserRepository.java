package com.example.elasticsearch.repository;

import com.example.elasticsearch.entity.UserDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends ElasticsearchRepository<UserDocument, Long> {
    List<UserDocument> findByAgeAndLove(Integer age, String love);
}

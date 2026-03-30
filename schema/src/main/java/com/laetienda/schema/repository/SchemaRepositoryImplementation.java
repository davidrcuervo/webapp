package com.laetienda.schema.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laetienda.lib.exception.NotValidCustomException;
import com.laetienda.model.schema.DbItem;
import com.laetienda.schema.repository.ItemRepository;
import com.laetienda.schema.repository.SchemaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

@Repository
public class SchemaRepositoryImplementation implements SchemaRepository{
    private final static Logger log = LoggerFactory.getLogger(SchemaRepositoryImplementation.class);

    @Autowired private ObjectMapper jsonMapper;
    @Autowired private ItemRepository repo;

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public <T> T create(Class<T> clazz, DbItem item) throws HttpStatusCodeException {
        log.debug("SCHEMA_REPO::create $clazzName: {}", clazz.getName());
        try{
            em.persist(item);
            return clazz.cast(item);
        }catch(Exception ex){
            log.error("SCHEMA_REPOSITORY::create. {}", ex.getMessage());
            log.trace("SCHEMA_REPOSITORY::create. {}", ex.getMessage(), ex);
            throw new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }

    @Override
    @Transactional
    public <T> T update(Class<T> clazz, DbItem item) throws NotValidCustomException {
        log.debug("SCHEMA_REPO::update $clazzName: {}", clazz.getName());
        T temp = clazz.cast(item);
        temp = em.merge(temp);
        em.persist(temp);
        return temp;
    }

    @Override
    public <T> T find(Class<T> clazz, Map<String, String> body) {
        log.debug("SCHEMA_REPO::find $clazzName: {}", clazz.getName());
        Map.Entry<String, String> entry = body.entrySet().stream().findFirst().get();
        String key = entry.getKey();
        String value = entry.getValue();

        String query = String.format("SELECT t FROM %s t WHERE t.%s = :value", clazz.getName(), key);
        log.debug("SCHEMA_REPO::find $query: {}", query);
        TypedQuery<T> jpaQuery = em.createQuery(query, clazz);
        jpaQuery.setParameter("value", value);

        try{
            return jpaQuery.getSingleResult();
        }catch(NoResultException ex){
            return null;
        }
    }

    @Override
    public <T> List<T> findByQuery(Class clazz, Map<String, String> body) throws NotValidCustomException {
        log.debug("SCHEMA_REPO::findByQuery. $clazz: {} | $query: {}", clazz.getName(), body.get("query"));

        TypedQuery<T> jpaQuery = em.createQuery(body.get("query"), clazz);
        return jpaQuery.getResultList();
    }

    @Override
    @Transactional
    public <T> void delete(Class<T> clazz, T item) {
        log.debug("SCHEMA_REPO::delete $clazzName: {}", clazz.getName());
        em.remove(item);
    }

    @Override
    public <T> T findById(Long id, Class<T> clazz) throws NotValidCustomException {
        log.debug("SCHEMA_REPO::findById $clazz: {}", clazz.getName());
        String query = String.format("SELECT t FROM %s t WHERE t.id = :id", clazz.getName());
        log.debug("SCHEMA_REPO::findById. $id: {}, $query: {}", id, query);

        TypedQuery<T> jpaQuery = em.createQuery(query, clazz);
        jpaQuery.setParameter("id", id);

        try {
            return jpaQuery.getSingleResult();
        }catch(NoResultException ex){
            log.debug("SCHEMA_REPO::findById. $error: {}", ex.getMessage());
            return null;
        }
    }

    @Transactional
    public boolean deleteUserById(String userId){
        log.debug("SCHEMA_REPO::deleteUserById. $userId: {}", userId);

        List<DbItem> readers =  repo.findByEditors(userId);
        readers.stream().map(item -> {
            item.removeEditor(userId);
            return item;
        }).forEach(item -> em.merge(item));

        List<DbItem> editors = repo.findByReaders(userId);
        editors.stream().map(item -> {
            item.removeReader(userId);
            return item;
        }).forEach(item -> em.merge(item));

        return true;
    }
}

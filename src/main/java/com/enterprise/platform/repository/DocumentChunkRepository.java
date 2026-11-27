
package com.enterprise.platform.repository;

import com.enterprise.platform.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DocumentChunkRepository
        extends JpaRepository<DocumentChunk, Long> {

    @Query("""
            SELECT chunk
            FROM DocumentChunk chunk
            JOIN FETCH chunk.document
            WHERE chunk.document.id = :documentId
            ORDER BY chunk.chunkIndex ASC
            """)
    List<DocumentChunk> findByDocumentIdOrderByChunkIndexAsc(
            @Param("documentId") Long documentId
    );

    @Query("""
            SELECT chunk
            FROM DocumentChunk chunk
            JOIN FETCH chunk.document
            WHERE chunk.document.id IN :documentIds
            ORDER BY chunk.document.id ASC, chunk.chunkIndex ASC
            """)
    List<DocumentChunk> findByDocumentIdInOrderByDocumentIdAscChunkIndexAsc(
            @Param("documentIds") List<Long> documentIds
    );

    void deleteByDocumentId(Long documentId);
}

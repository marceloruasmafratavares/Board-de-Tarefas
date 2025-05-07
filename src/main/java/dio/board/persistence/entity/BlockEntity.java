package dio.board.persistence.entity;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.function.Predicate;

import static dio.board.persistence.entity.BoardColumnKindEnum.CANCEL;
import static dio.board.persistence.entity.BoardColumnKindEnum.INITIAL;

@Data
public class BlockEntity {

    private Long id;
    private OffsetDateTime blockedAt;
    private String blockReason;
    private OffsetDateTime unblockedAt;
    private String unblockReason;
}

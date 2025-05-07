package dio.board.ui;

import dio.board.dto.BoardColumnInfoDTO;
import dio.board.persistence.entity.BoardColumnEntity;
import dio.board.persistence.entity.BoardEntity;
import dio.board.persistence.entity.CardEntity;
import dio.board.service.BoardColumnQueryService;
import dio.board.service.BoardQueryService;
import dio.board.service.CardQueryService;
import dio.board.service.CardService;
import lombok.AllArgsConstructor;

import java.sql.SQLException;
import java.util.Scanner;

import static dio.board.persistence.config.ConnectionConfig.getConnection;
import static dio.board.persistence.entity.BoardColumnKindEnum.INITIAL;

@AllArgsConstructor
public class BoardMenu {

    private final Scanner scanner = new Scanner(System.in).useDelimiter("\n");

    private final BoardEntity entity;
    public void execute() {
        try{
            System.out.printf("Bem vindo ao board %s, selecione a operação desejada\n", entity.getId());

            System.out.println("Bem vindo ao gerenciador de boards, escolha a opção desejada");
            var option = -1;
            while (true) {
                System.out.println("1 - Criar um card");
                System.out.println("2 - Mover um card");
                System.out.println("3 - Bloquear um card");
                System.out.println("4 - Desbloquear um card");
                System.out.println("5 - Cancelar um card");
                System.out.println("6 - Visualizar Board");
                System.out.println("7 - Visualizar colunas com cards");
                System.out.println("8 - Visualizar card");
                System.out.println("9 - Voltar para o menu anterior");
                System.out.println("10 - Sair");
                option = scanner.nextInt();
                switch (option) {
                    case 1 -> createCard();
                    case 2 -> moveCardToNextColumn();
                    case 3 -> blockCard();
                    case 4 -> unblockCard();
                    case 5 -> cancelCard();
                    case 6 -> showBoard();
                    case 7 -> showColumn();
                    case 8 -> showCard();
                    case 9 -> System.out.println("Voltando para o menu anterior");
                    case 10 -> System.exit(0);
                    default -> System.out.println("Opção inválida, informe uma opção do menu");
                }
            }
        }catch (SQLException ex){
        ex.printStackTrace();
        System.exit(0);
        }
    }

    private void createCard() throws SQLException {
         var card = new CardEntity();
         System.out.println("Informe o título do card");
         card.setTitle(scanner.next());
         System.out.println("Informe a descrição do card");
         card.setDescription(scanner.next());
         var initialColumn = entity.getBoardColumns().stream()
                 .filter(bc -> bc.getKind().equals(INITIAL))
                 .findFirst().orElseThrow();
         card.setBoardColumn(initialColumn);
         try(var connection = getConnection()){
             new CardService(connection).insert(card);
         }
    }

    private void moveCardToNextColumn() throws SQLException{
        System.out.println("Informe o id do card que deseja mover para a próxima coluna");
        var cardId = scanner.nextLong();
        var boardColumnInfo = entity.getBoardColumns().stream()
                .map(bc -> new BoardColumnInfoDTO(bc.getId(), bc.getOrder(), bc.getKind()))
                .toList();
        try(var connection = getConnection()){
            new CardService(connection).moveToNextColumn(cardId, boardColumnInfo);
        }   catch (RuntimeException ex){
            ex.printStackTrace();
        }
    }

    private void blockCard() throws SQLException{
        System.out.println("Informe o id do card que será bloqueado");
        var cardId = scanner.nextLong();
        System.out.println("Informe o motivo do bloqueio do card");
        var reason = scanner.next();
        var boardColumnInfo = entity.getBoardColumns().stream()
                .map(bc -> new BoardColumnInfoDTO(bc.getId(), bc.getOrder(), bc.getKind()))
                .toList();
        try(var connection = getConnection()){
            new CardService(connection).block(cardId, reason, boardColumnInfo);
        }   catch (RuntimeException ex){
            System.out.println(ex.getMessage());
        }
    }

    private void unblockCard() throws SQLException{
        System.out.println("Informe o id do card que será desbloqueado");
        var cardId = scanner.nextLong();
        System.out.println("Informe o motivo do desbloqueio do card");
        var reason = scanner.next();
        try(var connection = getConnection()){
            new CardService(connection).unblock(cardId, reason);
        }   catch (RuntimeException ex){
            ex.printStackTrace();
        }
    }

    private void cancelCard() throws SQLException{
        System.out.println("Informe o id do card que deseja mover para a coluna de cancelamento");
        var cardId = scanner.nextLong();
        var cancelColumn = entity.getCancelColumn();
        var boardColumnsInfo = entity.getBoardColumns().stream()
                .map(bc -> new BoardColumnInfoDTO(bc.getId(), bc.getOrder(), bc.getKind()))
                .toList();
        try(var connection = getConnection()){
            new CardService(connection).cancel(cardId, cancelColumn.getId(), boardColumnsInfo);
        }   catch (RuntimeException ex){
            ex.printStackTrace();
        }
    }

    private void showBoard() throws SQLException{
        try (var connection = getConnection()){
            var optional = new BoardQueryService(connection).showBoardDetails(entity.getId());
            optional.ifPresent(b -> {
                System.out.printf("Board [%s,%s]\n", b.id(), b.name());
                b.columns().forEach(c ->
                        System.out.printf("Coluna [%s] tipo: [%s] tem %s cards\n", c.name(), c.kind(), c.cardsAmount())
                );
            });
        }
    }

    private void showColumn() throws SQLException{
        var columnsIds = entity.getBoardColumns().stream().map(BoardColumnEntity::getId).toList();
        var selectedColumn = -1L;
        while (!columnsIds.contains(selectedColumn)){
            System.out.printf("Escolha uma coluna do Board %s\n", entity.getName());
            entity.getBoardColumns().forEach(c -> System.out.printf("%s - %s [%s]\n", c.getId(), c.getName(), c.getKind()));
            selectedColumn = scanner.nextLong();
        }
        try (var connection = getConnection()){
            var column = new BoardColumnQueryService(connection).findById(selectedColumn);
            column.ifPresent(co -> {
                System.out.printf("Coluna %s tipo %s\n", co.getName(), co.getKind());
                co.getCards().forEach(ca -> System.out.printf("Card %s - %s\nDescrição: %s\n",
                        ca.getId(), ca.getTitle(), ca.getDescription()));

            });
        }
    }

    private void showCard() throws SQLException{
        System.out.println("Informe o id do card que deseja visualizar");
        var selectedCardId = scanner.nextLong();
        try(var connection = getConnection()){
            new CardQueryService(connection).findById(selectedCardId)
                    .ifPresentOrElse(c ->{
                        System.out.printf("Card %s - %s.\n", c.id(), c.title());
                        System.out.printf("Descrição: %s\n", c.description());
                        System.out.println(c.blocked() ?
                                "Está bloqueado. Motivo: %s" + c.blockReason() :
                                "Não está bloqueado");
                        System.out.printf("Já foi bloqueado %s vezes\n", c.blocksAmount());
                        System.out.printf("Está no momento na coluna %s - %s\n", c.columnId(), c.columnName());
                    }, () -> System.out.printf("Não existe um card com o id %s\n", selectedCardId));
        }
    }
}

package net.woniper.board.test.service;

import net.woniper.board.BoardApplication;
import net.woniper.board.builder.EntityBuilder;
import net.woniper.board.domain.Board;
import net.woniper.board.domain.User;
import net.woniper.board.domain.type.AuthorityType;
import net.woniper.board.errors.support.BoardNotFoundException;
import net.woniper.board.errors.support.UserNotFoundException;
import net.woniper.board.service.BoardService;
import net.woniper.board.service.UserService;
import net.woniper.board.support.dto.BoardDto;
import net.woniper.board.test.config.TestDatabaseConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMethod;

import static org.junit.Assert.*;

/**
 * Created by woniper on 15. 2. 4..
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {BoardApplication.class, TestDatabaseConfig.class})
@WebAppConfiguration
@IntegrationTest("server.port=8888")
@Transactional
public class BoardServiceTest {

    @Autowired private UserService userService;
    @Autowired private BoardService boardService;

    @Autowired private ModelMapper modelMapper;

    private User admin;
    private User user;

    private Board adminBoard;
    private Board userBoard;

    @Before
    public void setUp() throws Exception {
        admin = userService.createUser(EntityBuilder.createUser(AuthorityType.ADMIN));
        user = userService.createUser(EntityBuilder.createUser(AuthorityType.USER));

        adminBoard = boardService.createBoard
                (modelMapper.map(EntityBuilder.createBoard(admin), BoardDto.Response.class), admin.getUsername());
        userBoard = boardService.createBoard
                (modelMapper.map(EntityBuilder.createBoard(user), BoardDto.Response.class), user.getUsername());
    }

    @Test
    public void test_getBoard() throws Exception {
        // given
        assertEquals(0, userBoard.getReadCount());

        // when
        Board board = boardService.getBoard(userBoard.getBoardId());

        // then
        assertEquals(1, board.getReadCount());
        assertEquals(userBoard.getBoardId(), board.getBoardId());
        assertEquals(userBoard.getTitle(), board.getTitle());
    }

    @Test(expected = UserNotFoundException.class)
    public void test_createBoard_UserNotFound() throws Exception {
        // given
        BoardDto boardDto = modelMapper.map(EntityBuilder.createBoard(user), BoardDto.class);

        // when
        boardService.createBoard(boardDto, "notUser");

        // then
        fail("UserNotFoundException");
    }

    @Test
    public void test_createBoard() throws Exception {
        // given
        Board newBoard = EntityBuilder.createBoard(user);
        BoardDto newBoardDto = modelMapper.map(newBoard, BoardDto.class);

        // when
        Board createBoard = boardService.createBoard(newBoardDto, user.getUsername());

        // then
        assertEquals(newBoard.getTitle(), createBoard.getTitle());
        assertEquals(newBoard.getContent(), createBoard.getContent());
    }

    @Test(expected = BoardNotFoundException.class)
    public void test_updateBoard_admin_notFoundException() throws Exception {
        // given
        Long boardId = Long.MAX_VALUE;
        BoardDto boardDto = modelMapper.map(adminBoard, BoardDto.class);
        String username = admin.getUsername();

        // when
        boardService.updateBoard(boardId, boardDto, username, RequestMethod.PUT.toString());

        // then
        fail("Board Not Found Exception");
    }

    @Test(expected = BoardNotFoundException.class)
    public void test_updateBoard_user_notFoundException() throws Exception {
        // given
        Long boardId = Long.MAX_VALUE;
        BoardDto boardDto = modelMapper.map(userBoard, BoardDto.class);
        String username = user.getUsername();

        // when
        boardService.updateBoard(boardId, boardDto, username, RequestMethod.PUT.toString());

        // then
        fail("Board Not Found Exception");
    }

    @Test
    public void test_updateBoard_patch_null() throws Exception {
        // given
        Long boardId = userBoard.getBoardId();
        BoardDto boardDto = modelMapper.map(userBoard, BoardDto.class);
        boardDto.setTitle(null);
        boardDto.setContent("updateContent");
        String username = user.getUsername();

        // when
        Board board = boardService.updateBoard(boardId, boardDto, username, RequestMethod.PATCH.toString());

        // then
        assertNotNull(board.getTitle());
        assertEquals(userBoard.getContent(), board.getContent());
    }

    @Test
    public void test_updateBoard_patch_not_null() throws Exception {
        // given
        Long boardId = userBoard.getBoardId();
        BoardDto boardDto = modelMapper.map(userBoard, BoardDto.class);
        boardDto.setTitle("updateTitle");
        boardDto.setContent("updateContent");
        String username = user.getUsername();

        // when
        Board board = boardService.updateBoard(boardId, boardDto, username, RequestMethod.PATCH.toString());

        // then
        assertEquals(boardDto.getTitle(), board.getTitle());
        assertEquals(boardDto.getContent(), board.getContent());
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_updateBoard_update_null() throws Exception {
        // given
        Long boardId = userBoard.getBoardId();
        BoardDto boardDto = modelMapper.map(userBoard, BoardDto.class);
        boardDto.setTitle(null);
        boardDto.setContent("updateContent");
        String username = user.getUsername();

        // when
        boardService.updateBoard(boardId, boardDto, username, RequestMethod.PUT.toString());

        // then
        fail("IllegalArgumentException");
    }

    @Test
    public void test_updateBoard_update_not_null() throws Exception {
        // given
        Long boardId = userBoard.getBoardId();
        BoardDto boardDto = modelMapper.map(userBoard, BoardDto.class);
        boardDto.setTitle("updateTitle");
        boardDto.setContent("updateContent");
        String username = user.getUsername();

        // when
        Board board = boardService.updateBoard(boardId, boardDto, username, RequestMethod.PUT.toString());

        // then
        assertEquals(boardDto.getTitle(), board.getTitle());
        assertEquals(boardDto.getContent(), board.getContent());
    }

    @Test
    public void test_deleteBoard_admin이_다른_user_Board_삭제() throws Exception {
        // given
        Long userBoardId = userBoard.getBoardId();
        String adminUsername = admin.getUsername();

        // when
        boolean isDelete = boardService.deleteBoard(userBoardId, adminUsername);

        // then
        assertEquals(true, isDelete);
    }

    @Test
    public void test_deleteBoard_user가_다른_user_Board_삭제() throws Exception {
        // given
        Long adminBoardId = adminBoard.getBoardId();
        String username = user.getUsername();

        // when
        boolean isDelete = boardService.deleteBoard(adminBoardId, username);

        // then
        assertEquals(false, isDelete);
    }

    @Test
    public void test_deleteBoard_자신이쓴_Board_삭제() throws Exception {
        // given
        Long userBoardId = userBoard.getBoardId();
        String username = user.getUsername();

        // when
        boolean isDelete = boardService.deleteBoard(userBoardId, username);

        // then
        assertEquals(true, isDelete);
    }
}
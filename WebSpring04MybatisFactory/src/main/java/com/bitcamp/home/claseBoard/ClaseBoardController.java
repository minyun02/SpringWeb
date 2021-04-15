package com.bitcamp.home.claseBoard;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ClaseBoardController {
	@Autowired
	SqlSession sqlSession;
	
	@Autowired
	private DataSourceTransactionManager transactionManager;
	
	//글목록
	@RequestMapping("/claseList")
	public ModelAndView claseList() {
		ModelAndView mav = new ModelAndView();
		ClaseBoardDAOImp dao = sqlSession.getMapper(ClaseBoardDAOImp.class);
		
		mav.addObject("list", dao.claseAllRecord());
		
		mav.setViewName("claseBoard/claseList");
		return mav;
	}
	//글쓰기 폼
	@RequestMapping("/claseWrite")
	public String claseWrite() {
		return "claseBoard/claseWrite";
	}
	//글쓰기 - DB
	@RequestMapping(value="/claseWriteOk", method=RequestMethod.POST)
	public ModelAndView claseWriteOk(ClaseBoardDTO vo, HttpServletRequest req) {
		vo.setIp(req.getRemoteAddr());
		vo.setUserid((String)req.getSession().getAttribute("logId"));
		
		ClaseBoardDAOImp dao = sqlSession.getMapper(ClaseBoardDAOImp.class);
		int cnt = dao.claseInsert(vo);
		
		ModelAndView mav = new ModelAndView();
		if(cnt>0) {
			mav.setViewName("redirect:claseList");
		}else {
			mav.setViewName("redirect:claseWrite");
		}
		return mav;
	}
	//글 내용보기
	@RequestMapping("/claseView")
	public ModelAndView claseView(int no) {
		ClaseBoardDAOImp dao = sqlSession.getMapper(ClaseBoardDAOImp.class);
		
		
		ModelAndView mav = new ModelAndView();
		dao.hitCount(no); //게시물을 눌려서 들어가면 조회수를 올려줘야지!
		mav.addObject("dto", dao.claseSelect(no));
		mav.setViewName("claseBoard/claseView");
		return mav;
	}
	//답글쓰기 폼으로 이동
	@RequestMapping("/claseWriteForm")
	public ModelAndView claseWriteForm(int no) {
		ModelAndView mav = new ModelAndView();
		mav.addObject("no", no);
		mav.setViewName("claseBoard/claseWriteForm");
		return mav;
	}
	//답글쓰기
	@RequestMapping(value="/claseWriteFormOk", method=RequestMethod.POST)
	@Transactional(rollbackFor= {Exception.class, RuntimeException.class})
	public ModelAndView claseWriteFormOk(ClaseBoardDTO dto, HttpSession session, HttpServletRequest req) {
		DefaultTransactionDefinition def = new DefaultTransactionDefinition();
		def.setPropagationBehavior(DefaultTransactionDefinition.PROPAGATION_REQUIRED);
		
		TransactionStatus status = transactionManager.getTransaction(def);
		
		
		dto.setIp(req.getRemoteAddr());
		dto.setUserid((String)session.getAttribute("logId"));
		
		ModelAndView mav = new ModelAndView();
		ClaseBoardDAOImp dao = sqlSession.getMapper(ClaseBoardDAOImp.class);
		try {
			//1.원글번호의 ref, step, lvl를 가져온다(select)
			ClaseBoardDTO orgDto = dao.origInfor(dto.getNo());
			
			//2.lvl증가 : 원글번호가 같고 lvl이 원글번호의 lvl보다 크면 1증가 한다
			int lvlCnt = dao.lvlcount(orgDto);
			System.out.println(lvlCnt+"개 lvl 업데이트 완료");
			
			//3. 답글추가
			dto.setRef(orgDto.getRef());
			dto.setStep(orgDto.getStep()+1);
			dto.setLvl(orgDto.getLvl()+1);
			
			int cnt = dao.claseDataInsert(dto);
			mav.addObject("no", dto.getNo());
			if(cnt>0) {//정상 실행
				//원글보기
				mav.setViewName("redirect:claseView");
				transactionManager.commit(status);
			}else {//something went wrong~
				mav.setViewName("redirect:claseWriteForm");
				transactionManager.rollback(status);
			}
		}catch(Exception e) {
			mav.addObject("no", dto.getNo());
			mav.setViewName("redirect:claseWriteForm");
			System.out.println("답글쓰기 에러발생--> 롤백!!!");
		}
		return mav;
	}
}

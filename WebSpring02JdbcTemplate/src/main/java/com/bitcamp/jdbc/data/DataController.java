package com.bitcamp.jdbc.data;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class DataController {
	//자료실 목록 불러오기
	@RequestMapping("/dataList")
	public ModelAndView dataList() {
		DataDAO dao = new DataDAO();
		ModelAndView mav = new ModelAndView();
		
		mav.addObject("dataList", dao.allList());
		mav.setViewName("data/dataList");
		return mav;
	}
	//첫번째 파일 업로드 폼으로 이동
	@RequestMapping("/dataWrite1")
	public String dataWrite1() {
		return "data/dataWrite1";
	}
	// 파일 업로드1========================
	@RequestMapping(value="/upload1", method=RequestMethod.POST)
	public ModelAndView dataWrite1Ok(
			@RequestParam("title") String title,
			@RequestParam("content") String content,
			@RequestParam("filename1") MultipartFile filename1,
			@RequestParam("filename2") MultipartFile filename2,
			HttpServletRequest req
			) {
		String userid = (String)req.getSession().getAttribute("logId");
		String ip = req.getRemoteAddr();
		
		//파일 업로드
		String path = req.getSession().getServletContext().getRealPath("/upload");		//저장위치 구하기
		String paramName = filename1.getName(); //매개 변수
		String oriName = filename1.getOriginalFilename(); //원파일명
		System.out.println("매개변수이름-?>"+paramName+", 원파일명==>"+oriName);
		
		// transferTo() : 실제 업로드 발생
		try {
			if(oriName!=null && !oriName.equals("")) {//	파일객체 만들때 경로, 파일명 필요
				filename1.transferTo(new File(path, oriName)); //업로드
			}
		}catch(Exception e) {
			System.out.println("파일 업로드1 에러발생...");
			e.printStackTrace();
		}
		// 두번째 파일
		String paramName2 = filename2.getName();
		String oriName2 = filename2.getOriginalFilename();
		
		try {
			if(oriName2!=null && !oriName2.equals("")) {
				filename2.transferTo(new File(path, oriName2));
			}
		}catch(Exception e) {
			System.out.println("2번째 파일 업로드 실패..");
			e.printStackTrace();
		}
		
		//데이터베이스 처리
		DataVO vo = new DataVO();
		vo.setTitle(title);
		vo.setContent(content);
		vo.setUserid(userid);
		vo.setIp(ip);
		vo.setFilename1(oriName);
		vo.setFilename2(oriName2);
		
		DataDAO dao = new DataDAO();
		int result = dao.dataInsert1(vo);
		ModelAndView mav = new ModelAndView();
		
		//레코드 추가 실패시 파일을 삭제...
		if(result<=0) {
			//첫번째 파일 삭제
			if(oriName!=null) {
				File f = new File(path, oriName);
				f.delete();
			}
			//두번째 파일 삭제
			if(oriName2!=null) {
				File f = new File(path, oriName2);
				f.delete();
			}
			mav.setViewName("redirect:dataWrite1");
		}else {
			mav.setViewName("redirect:dataList");
		}
		return mav;
	}
	// 파일 업로드2 (form의 name이 같을때) ========================
	@RequestMapping("/dataWrite2")
	public String dataWrite2() {
		return "data/dataWrite2";
	}
	@RequestMapping(value="upload2", method=RequestMethod.POST)
	public ModelAndView dataUpload2(DataVO vo, HttpServletRequest req) {
		// vo -> 제목, 글내용
		vo.setUserid((String)req.getSession().getAttribute("logId"));		// 아이디
		vo.setIp(req.getRemoteAddr());		// 아이피
		
		String path = req.getSession().getServletContext().getRealPath("/upload");
		
		// request 객체를 MultipartHttpServletRequest객체로 만들어서 파일 업로드를 한다.
		MultipartHttpServletRequest mr = (MultipartHttpServletRequest)req;
		
		//mr객체에서 업로드 파일 목록을 구한다.
		List<MultipartFile> files = mr.getFiles("filename");
		
		List<String> uploadFilename = new ArrayList<String>();
		if(files.size()>0) {//첨부파일이 있을때
			for(MultipartFile mf : files){//첨부파일 수 만큼 반복
				String orgFilename = mf.getOriginalFilename();
				if(!orgFilename.equals("")) {	
						File f = new File(path,  orgFilename);
						int i = 1;
						while(f.exists()) {; // 있으면 : true, 없으면 : false
							int point = orgFilename.lastIndexOf("."); // 마지막 . 위치가 필요
							String name = orgFilename.substring(0, point); //파일명
							String extName = orgFilename.substring(point+1); //확장자
							
							f = new File(path, name+"_"+ i++ +"."+extName);
						
						}//while
						
						//업로드
						try {
							mf.transferTo(f);
						}catch(Exception e) {
							System.out.println("파일 업로드 실패 144");
						}
						//변경된 파일명
						uploadFilename.add(f.getName());
				}//if
			}//for
		}//if
		
		// database 작업
		vo.setFilename1(uploadFilename.get(0));
		if(uploadFilename.size()==2) {
			vo.setFilename2(uploadFilename.get(1));
		}
		
		DataDAO dao = new DataDAO();
		int cnt = dao.dataInsert1(vo);
		ModelAndView mav = new ModelAndView();
		if(cnt>0) {//추가 성공
			mav.setViewName("redirect:dataList");
		}else {//실패
			//파일지우고
			for(String delFile : uploadFilename) {
				File del = new File(path, delFile);
				del.delete();
			}
			mav.setViewName("redirect:dataWrite2");
		}
		return mav;
	}
}

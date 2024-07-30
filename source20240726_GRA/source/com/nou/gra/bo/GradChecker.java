package com.nou.gra.bo;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspWriter;

import com.acer.db.DBManager;
import com.acer.util.Utility;
import com.nou.gra.dao.GRAT003DAO;
import com.nou.gra.dao.GRAT028DAO;
import com.nou.gra.dao.GRAT029DAO;
import com.nou.gra.dao.GRAT030DAO;
import com.nou.gra.dao.GRAT031DAO;
import com.nou.gra.po.ApplyStudents;
import com.nou.gra.po.CheckingLog;
import com.nou.gra.po.CrdChange;
import com.nou.gra.po.CrsnoAdopt;
import com.nou.gra.po.CrsnoInfo;
import com.nou.gra.po.GraParameters;
import com.nou.gra.po.MultiCrsno;
import com.nou.gra.po.Parameters;
import com.nou.gra.po.VerifyCommon;
import com.nou.gra.po.VerifyDetail;
import com.nou.gra.po.VerifyFaculty;
import com.nou.pop.signup.tool.Transformer;

public abstract class GradChecker implements Checker {
	public static final String AUTO_AUDIT_STATUS = "AUTO_AUDIT_STATUS";
	public static final String GRAD_REEXAM_STATUS = "GRAD_REEXAM_STATUS";
	public static final String AUTO_AUDIT_UNQUAL_CAUSE = "AUTO_AUDIT_UNQUAL_CAUSE";
	public static final String GRAD_REEXAM_UNQUAL_CAUSE = "GRAD_REEXAM_UNQUAL_CAUSE";

	public static final String WAITING = "1";// 待審查
	public static final String PASS = "2";// 通過
	public static final String NOT_PASS = "3";// 不通過

	public static final String ASYS_OPEN = "1";// 空大學制

	public static final String SINGLE = "01";// 單主修
	public static final String DOUBLE = "02";// 雙主修
	public static final String OLD_DOUBLE = "05";// 舊制雙主修

	public static final String FIRST_VERIFY = "1";// 初審狀態
	public static final String LAST_VERIFY = "2";// 複審狀態

	public static final String COMMON_PRINT_1 = "1";// 共同課程表1
	public static final String COMMON_PRINT_2 = "2";// 共同課程表2
	public static final String COMMON_PRINT_3 = "3";// 共同課程表3
	public static final String COMMON_PRINT_4 = "4";// 共同課程表4

	public static final String FROM_SUMMER = "3";// 暑修課程
	public static final String IS_SUMMER = "Y";// 是否為暑修課程
	public static final String TO_SUMMER = "5";// 暑修課程
	public static final String TO_THIS_YEAR = "4";// 當學年期修的課

	public static final String IS_VALID = "Y";// 有效科目
	public static final String NOT_VALID = "N";// 無效科目
	public static final String IS_MULTI_CRSNO_PART = "B";// 無效科目 多科取部份科目
	public static final String IS_MULTI_CRSNO_NCRD = "G";// 無效科目 多科取N學分
	
	public static final String NOT_VALID_NAME = "無效科目"; // 無效科目
	public static final String IS_MULTI_CRSNO_PART_NAME = "多科取N科";// 無效科目 
	public static final String IS_MULTI_CRSNO_NCRD_NAME = "多科取N學分";// 無效科目 

	public static final String IS_ADOPT = "Y";// 採計
	public static final String NOT_ADOPT = "N";// 不採計

	public static final String IS_ADD_CRD_TO_TOTAL = "Y";// 將剩餘學分加入總學分

	public static final String ADOPT_NAME = "_adopt";// 無效科目

	public static final String CAN_GRAD = "5";// 畢業狀態
	public static final String CAN_NOT_GRAD = "2";// 在籍狀態
	
	

	protected JspWriter out;
	protected DBManager dbManager;
	protected Hashtable requestMap;
	protected HttpSession session;
	protected Connection conn;

	protected Parameters parameters;// 全校性參數
	protected Hashtable graParamHt; // 全校性畢業參數
	protected List deadlineList;// 科目有效期限參數
	protected List crdChangeList;// 科目學分變動參數
	protected List adoptCrdList;// 採計他系學分上限
	protected List multiCrsnoList;// 多科取一科或N學分參數
	protected Map commonKind;// 共同課程科目群組對照表
	protected List facultyCodeList;// 學系代碼

	protected Transformer transformer;
	protected CrdAdopt crdAdopt;

	protected int remainCrd;// 記錄多科取一科的剩餘學分
	protected Map reducedCrdMap;
	protected int old;
	protected int new1;
	protected int new2;
	protected Map obligatoryMap;
	protected Map obligatoryAllMap;
	protected Map summerMap;
	protected int multiCrd;// 記錄多科取一科的學分
	protected int nouCrd;// 採認學分
	protected Map nouMap;// 採認科目
	protected Map crsnoMap;// 新舊科目對照(新)
	protected Map CommonCrsnoMap;// 兩系共開
	/** 通識課程資料 */
	protected Vector gelVector;

	protected String errMsg = ""; // 錯誤訊息
	
	public String getErrMsg() {
		return errMsg;
	}

	public void setErrMsg(String errMsg) {
		this.errMsg = errMsg;
	}

	public GradChecker(JspWriter out, DBManager dbManager, Hashtable requestMap, HttpSession session) throws Exception {
		this.out = out;
		this.dbManager = dbManager;
		this.requestMap = requestMap;
		this.session = session;
		// this.conn = dbManager.getConnection(AUTCONNECT.mapConnect("GRA",
		// session));
		this.conn = ConnectionWithoutPool.getConnection();

		// 取出全校性參數
		parameters = ParameterUtil.getParameters(this.dbManager, this.requestMap, this.conn);
		if (parameters != null) {
			graParamHt = parameters.getGraHt();
		}
		
		deadlineList = ParameterUtil.getDeadline(this.dbManager, this.requestMap, this.conn);// 有效期限
		crdChangeList = ParameterUtil.getCrdChange(this.dbManager, this.requestMap, this.conn);// 學分變動
		// 相同性質科目

		adoptCrdList = ParameterUtil.getAdoptCrd(this.dbManager, this.requestMap, this.conn);// 他系採計
		multiCrsnoList = ParameterUtil.getMultiCrsno(this.dbManager, this.requestMap, this.conn);// 多科取一科或N學分參數

		transformer = new Transformer();
		crdAdopt = new CrdAdopt(new Hashtable());
		commonKind = ParameterUtil.getCommonKind(this.dbManager, this.conn);
		facultyCodeList = ParameterUtil.getFacultyCodeList(this.dbManager, this.conn);

		reducedCrdMap = ParameterUtil.getReducedCrdMap(this.dbManager, this.requestMap, this.conn);// 減修學分
		obligatoryMap = ParameterUtil.getObligatoryMap(this.dbManager, this.requestMap, this.conn);// 必修科目
		obligatoryAllMap = ParameterUtil.getObligatoryAllMap(this.dbManager, this.requestMap, this.conn);// 必修科目群組new
		summerMap = ParameterUtil.getSummerInfoMap(this.dbManager, this.conn);// 暑期開課卻非暑修課程
		nouMap = ParameterUtil.getNouInfoMap(this.dbManager, this.requestMap, this.conn);
		crsnoMap = ParameterUtil.getCrsnoInfoMap(this.dbManager, this.requestMap, this.conn);// 新舊科目對照
		CommonCrsnoMap = ParameterUtil.getCommonCrsnoMap(this.dbManager, this.conn);// 兩系共開
		gelVector = ParameterUtil.getGelVector(this.dbManager, this.conn); //通識課程
	}

	public void run() throws Exception {
		// do nothing
	}

	protected void doVerify(String status) throws Exception {
		// 取出當學年期所有學系初審狀態、共同科初審狀態、複審狀態為待審查的所有學生
		List list = DataGetter.getApplyStudents(dbManager, conn, requestMap, status);
		if (list != null && list.size() > 0) {
			this.session.setAttribute("check_total_count", new Integer(list.size()));
			for (int i = 0; i < list.size(); i++) {
				ApplyStudents aStudent = (ApplyStudents) list.get(i);
				
				remainCrd = 0;// 剩餘學分總數
				multiCrd = 0;
				int summerWithoutAdopt = 0;// 暑修不採計學分總數
				old = 0;// 必修科目行政學
				new1 = 0;// 必修科目行政學(一)(上)
				new2 = 0;// 必修科目行政學(二)(下)
				nouCrd = 0;// 採認學分

				List logList = new ArrayList();// 記錄檔
				List mList = new ArrayList();// 多科取一科或N學分的暫存區
				// List studyingCrsnoList =
				// ParameterUtil.getStudyingCrsno(dbManager, conn, aStudent);//
				// 當學年期修的課
				List studyingCrsnoList = new ArrayList();// 當學年期修的課
				StringBuffer errorMsg = new StringBuffer();// 不通過原因
				StringBuffer errorMsgCode = new StringBuffer();// 不通過原因代碼

				// 單主修
				if (SINGLE.equals(aStudent.getAppGradType())) {
					this.processKind01(aStudent, errorMsg, errorMsgCode, logList, mList, summerWithoutAdopt, studyingCrsnoList, status);
				} else if (DOUBLE.equals(aStudent.getAppGradType())) {// 雙主修
					this.processKind02(aStudent, errorMsg, errorMsgCode, logList, mList, summerWithoutAdopt, studyingCrsnoList, status);
				} else if (OLD_DOUBLE.equals(aStudent.getAppGradType())) {// 舊制雙主修
					this.processKind05(aStudent, errorMsg, errorMsgCode, logList, mList, summerWithoutAdopt, studyingCrsnoList, status);
				}

				System.out.println(i+1 + "/" + list.size());
				this.session.setAttribute("check_current_count", new Integer(i + 1));
			}
		}
	}

	protected void processKind01(ApplyStudents aStudent, StringBuffer errorMsg, StringBuffer errorMsgCode, List logList, List mList, int summerWithoutAdopt,
			List studyingCrsnoList, String status) throws Exception {
		// 取得學生修得的所有科目
		List aCrsno = DataGetter.getAllCrsno(dbManager, conn, aStudent, false);
		//by poto 計算修得學分
		int passCrd =0;
		StringBuffer CrsnoList = new StringBuffer() ;
		if (aCrsno != null && aCrsno.size() > 0) {
			for (int j = 0; j < aCrsno.size(); j++) {
				CrsnoInfo ci = (CrsnoInfo) aCrsno.get(j);
								 
				if ("1".equals(ci.getGetManner())) {
					passCrd += Integer.parseInt(ci.getCrd());
				}
				
				if ("3".equals(ci.getGetManner())) {
					ci.setGetManner("2");
				}
				
				if(CrsnoList.length()!=0){					
					CrsnoList.append(","+ci.getCrsno());
				}else{					
					CrsnoList.append(ci.getCrsno());
				}
				
				// 判斷是否為暑修課程
				String key = ci.getAyear() + ci.getSms() + ci.getCrsno();
				if ("1".equals(ci.getSms()) || "2".equals(ci.getSms()) || "2".equals(ci.getGetManner()) || summerMap.get(key) != null) {
					// 記錄科目歸屬學系的LOG
					CheckingLog cLog = new CheckingLog();
					cLog.setAyear(aStudent.getAyear());// 申請學年
					cLog.setSms(aStudent.getSms());// 申請學期
					cLog.setStno(ci.getStno());
					cLog.setKind(status);
					cLog.setFacultyCode(ci.getFacultyCode());
					cLog.setCrsno(ci.getCrsno());
					cLog.setCrd(this.getCrsnoNewCrd(ci, cLog.getFacultyCode()));
					cLog.setGetAyear(ci.getAyear());// 科目取得學年
					cLog.setGetSms(ci.getSms());// 科目取得學期

					// 判斷是否為當學年期修的課(這邊不會有暑修的課程)
					if (status.equals(FIRST_VERIFY)) {
						cLog.setGetManner(ci.getGetManner());
					} else if (status.equals(LAST_VERIFY)) {
						cLog.setGetManner(ci.getGetManner());
					}

					// 判斷科目是否有效
					if (GradUtil.isCrsnoValid(deadlineList, cLog)) {
						cLog.setIsValid(IS_VALID);
					} else {
						cLog.setIsValid(NOT_VALID);
					}

					cLog.setIsAdopt(NOT_ADOPT);

					// 相同性質科目
					if (IS_VALID.equals(cLog.getIsValid())) {
						String tempKey = aStudent.getStno() + cLog.getGetAyear() + cLog.getGetSms() + cLog.getCrsno() + cLog.getGetManner();
						if (this.crsnoMap.get(tempKey) != null) {
							cLog.setIsValid(NOT_VALID);
						}
					}

					// 判斷科目是否為多科取部分科目
					MultiCrsno mc = GradUtil.isMultiCrsno(multiCrsnoList, aStudent, cLog);
					if (mc == null) {
						logList.add(cLog);
					} else {
						cLog.setMc(mc);
						mList.add(cLog);
						multiCrd += Integer.parseInt(cLog.getCrd());
					}
				} else {
					summerWithoutAdopt += Integer.parseInt(ci.getCrd());
				}
			}
		}

		// 取得學生暑修與採計的所有科目
		List adoptList = DataGetter.getAdoptCrsno(dbManager, conn, aStudent);
		if (adoptList != null && adoptList.size() > 0) {
			for (int j = 0; j < adoptList.size(); j++) {
				CrsnoAdopt ca = (CrsnoAdopt) adoptList.get(j);
                //by poto 20090623 初選部要排除抵免的
				// 初審排掉當學年期的科目
				//if (this.isThisYear(status, aStudent, ca.getGetAyear(), ca.getGetSms())) {
				//	continue;
				//}
				if ("1".equals(ca.getGetManner())) {
					passCrd += Integer.parseInt(ca.getCrd());
				}
				if ("3".equals(ca.getGetManner())) {
					ca.setGetManner("2");
				}				
				if(CrsnoList.length()!=0){					
					CrsnoList.append(","+ca.getCrsno());
				}else{					
					CrsnoList.append(ca.getCrsno());
				}
				// 記錄科目歸屬學系的LOG
				CheckingLog cLog = new CheckingLog();
				cLog.setAyear(aStudent.getAyear());// 申請學年
				cLog.setSms(aStudent.getSms());// 申請學期
				cLog.setStno(ca.getStno());
				cLog.setKind(status);

				boolean isSummerMajor = false;
				if (ca.getFacultyCode() != null && !"".equals(ca.getFacultyCode().trim())) {
					cLog.setFacultyCode(ca.getFacultyCode());
					if (IS_SUMMER.equals(ca.getIsSummer())) {
						isSummerMajor = true;
					}
				} else {
					cLog.setFacultyCode(ca.getAdoptFaculty());
				}

				cLog.setCrsno(ca.getCrsno());
				cLog.setCrd(this.getCrsnoNewCrd(ca));
				cLog.setGetAyear(ca.getGetAyear());// 科目取得學年
				cLog.setGetSms(ca.getGetSms());// 科目取得學期

				// 判斷是否為當學年期修的課
				if (status.equals(FIRST_VERIFY)) {
					if (IS_SUMMER.equals(ca.getIsSummer())) {
						cLog.setGetManner(TO_SUMMER);
					} else {
						cLog.setGetManner(ca.getGetManner());
					}
				} else if (status.equals(LAST_VERIFY)) {
					if (IS_SUMMER.equals(ca.getIsSummer())) {
						cLog.setGetManner(TO_SUMMER);
					} else {
						cLog.setGetManner(ca.getGetManner());
					}
				}

				// 判斷科目是否有效
				if (GradUtil.isCrsnoValid(deadlineList, cLog)) {
					cLog.setIsValid(IS_VALID);
				} else {
					cLog.setIsValid(NOT_VALID);
				}

				// 判斷暑修課程是否為主修學系
				if (isSummerMajor) {
					cLog.setIsAdopt(NOT_ADOPT);
				} else {
					// 20090106新增, 兩系共開於cout103還是會設定成一主多採形式, 但書面上還是會記錄兩系皆為主開
					// 於Gra002m申請時若決定將該科採計於其他科時, 這邊會判斷兩系共開, 並將其改為主修學系
					if (this.CommonCrsnoMap.get(cLog.getGetAyear() + cLog.getGetSms() + cLog.getCrsno() + "_COMMON") != null) {
						String commonFaculty = (String) this.CommonCrsnoMap.get(cLog.getGetAyear() + cLog.getGetSms() + cLog.getCrsno() + "_COMMON");
						String planFaculty = (String) this.CommonCrsnoMap.get(cLog.getGetAyear() + cLog.getGetSms() + cLog.getCrsno() + "_PLAN");
						if (aStudent.getGradMajorFaculty().equals(commonFaculty) || aStudent.getGradMajorFaculty().equals(planFaculty)) {
							cLog.setIsAdopt(NOT_ADOPT);
						} else {
							cLog.setIsAdopt(IS_ADOPT);
						}
					} else {
						cLog.setIsAdopt(IS_ADOPT);
					}
				}

				// 相同性質科目
				if (IS_VALID.equals(cLog.getIsValid())) {
					String tempKey = aStudent.getStno() + cLog.getGetAyear() + cLog.getGetSms() + cLog.getCrsno() + cLog.getGetManner();
					if (this.crsnoMap.get(tempKey) != null) {
						cLog.setIsValid(NOT_VALID);
					}
				}

				// 判斷科目是否為多科取部分科目
				MultiCrsno mc = GradUtil.isMultiCrsno(multiCrsnoList, aStudent, cLog);
				if (mc == null) {
					logList.add(cLog);
				} else {
					cLog.setMc(mc);
					mList.add(cLog);
					multiCrd += Integer.parseInt(cLog.getCrd());
				}
			}
		}
		
		
		// 將多科取部份科目或N學分的科目加到logList
		remainCrd += GradUtil.processMultiCrsno(mList, logList);
		
		// 開始計算學分
		int adopt = 0;// 申請歸併
		int major = 0;// 主修學系
		int majorPass = 0;// 主修學系修得學分數
		int summer = 0;// 暑修主修學系
		int summerAdopt = 0;// 暑修申請歸併
		int summerOther = 0;// 暑修其他選修
		int summerCommon = 0;// 暑修共同課程
		int summerCommonAdopt = 0;// 暑修共同課程申請歸併
		int popAdopt = 0;// 推廣申請歸併
		int popMajor = 0;// 推廣主修學系
		int popCommonMajor = 0;// 推廣共同課程主修學系
		int popCommonAdopt = 0;// 推廣共同課程申請歸併
		int popOther = 0;// 推廣其他選修
		int other = 0;// 其他選修
		int common = 0;// 共同課程
		int commonAdopt = 0;// 共同課程申請歸併
		boolean graPassCcs109Yn =  false; //判斷是否有109的CCS
		Map resultMap = new HashMap();
		Map commonMap = new HashMap();

		// 刪除Log
		StringBuffer condition = new StringBuffer();
		condition.append("AYEAR = '" + aStudent.getAyear() + "' ");
		condition.append("AND SMS = '" + aStudent.getSms() + "' ");
		condition.append("AND STNO = '" + aStudent.getStno() + "' ");

		GRAT030DAO grat030Dao = new GRAT030DAO(dbManager, conn);
		grat030Dao.delete(condition.toString() + "AND KIND = '" + status + "' ");

		GRAT028DAO grat028Daod = new GRAT028DAO(dbManager, conn);
		grat028Daod.delete(condition.toString() + "AND KIND = '" + status + "' ");

		GRAT029DAO grat029Daod = new GRAT029DAO(dbManager, conn);
		grat029Daod.delete(condition.toString() + "AND KIND = '" + status + "' ");

		GRAT031DAO grat031Daod = new GRAT031DAO(dbManager, conn);
		grat031Daod.delete(condition.toString() + "AND KINDS = '" + status + "' ");

		if (logList != null && logList.size() > 0) {
			for (int j = 0; j < logList.size(); j++) {
				CheckingLog cLog = (CheckingLog) logList.get(j);
				// 科目有效時才計算學分
				if (IS_VALID.equals(cLog.getIsValid()) || IS_MULTI_CRSNO_NCRD.equals(cLog.getIsValid()) || IS_MULTI_CRSNO_PART.equals(cLog.getIsValid())) {
					//共同科目
				    if ("90".equals(cLog.getFacultyCode()) && NOT_ADOPT.equals(cLog.getIsAdopt())
							&& ("1".equals(cLog.getGetManner()) || "2".equals(cLog.getGetManner()))) {
						common += Integer.parseInt(cLog.getCrd());// 共同課程
						GradUtil.putCommonMap(commonKind, commonMap, cLog, COMMON_PRINT_4);
					} else if ("90".equals(cLog.getFacultyCode()) && IS_ADOPT.equals(cLog.getIsAdopt())
							&& ("1".equals(cLog.getGetManner()) || "2".equals(cLog.getGetManner()))) {
						commonAdopt += Integer.parseInt(cLog.getCrd());// 共同課程申請歸併
						GradUtil.putCommonMap(commonKind, commonMap, cLog, COMMON_PRINT_4);
					} else if ("90".equals(cLog.getFacultyCode()) && TO_SUMMER.equals(cLog.getGetManner()) && NOT_ADOPT.equals(cLog.getIsAdopt())) {
						summerCommon += Integer.parseInt(cLog.getCrd());// 暑修共同課程
						GradUtil.putCommonMap(commonKind, commonMap, cLog, COMMON_PRINT_4);
					} else if ("90".equals(cLog.getFacultyCode()) && TO_SUMMER.equals(cLog.getGetManner()) && IS_ADOPT.equals(cLog.getIsAdopt())) {
						summerCommonAdopt += Integer.parseInt(cLog.getCrd());// 暑修共同課程申請歸併
						GradUtil.putCommonMap(commonKind, commonMap, cLog, COMMON_PRINT_4);	
					} else if ("90".equals(cLog.getFacultyCode()) && "3".equals(cLog.getGetManner()) && NOT_ADOPT.equals(cLog.getIsAdopt())) {
						popCommonMajor += Integer.parseInt(cLog.getCrd());// 推廣共同課程主修學系
						GradUtil.putCommonMap(commonKind, commonMap, cLog, COMMON_PRINT_4);
					} else if ("90".equals(cLog.getFacultyCode()) && "3".equals(cLog.getGetManner()) && IS_ADOPT.equals(cLog.getIsAdopt())) {
						popCommonAdopt += Integer.parseInt(cLog.getCrd());// 推廣共同課程申請歸併
						GradUtil.putCommonMap(commonKind, commonMap, cLog, COMMON_PRINT_4);	
					}else 
					//一般科目	
					if (aStudent.getGradMajorFaculty().equals(cLog.getFacultyCode()) && NOT_ADOPT.equals(cLog.getIsAdopt())
							&& ("1".equals(cLog.getGetManner()) || "2".equals(cLog.getGetManner()))) {
						major += Integer.parseInt(cLog.getCrd());// 主修學系
						majorPass += ("1".equals(cLog.getGetManner()))?Integer.parseInt(cLog.getCrd()):0;// 主修實得學系
						GradUtil.putResultMap(resultMap, cLog, cLog.getIsAdopt());
					} else if (aStudent.getGradMajorFaculty().equals(cLog.getFacultyCode()) && IS_ADOPT.equals(cLog.getIsAdopt())
							&& ("1".equals(cLog.getGetManner()) || "2".equals(cLog.getGetManner()))) {
						adopt += Integer.parseInt(cLog.getCrd());// 申請歸併
						majorPass += Integer.parseInt(cLog.getCrd());// 主修實得主修學系     /* 20240718主修實得學分含採計他系學分數 */  
						GradUtil.putResultMap(resultMap, cLog, cLog.getIsAdopt());
					} else if (!aStudent.getGradMajorFaculty().equals(cLog.getFacultyCode())
							&& ("1".equals(cLog.getGetManner()) || "2".equals(cLog.getGetManner()))) {
						other += Integer.parseInt(cLog.getCrd());// 其他選修
						GradUtil.putResultMap(resultMap, cLog, cLog.getIsAdopt());					
					} else if (aStudent.getGradMajorFaculty().equals(cLog.getFacultyCode()) && TO_SUMMER.equals(cLog.getGetManner())
							&& NOT_ADOPT.equals(cLog.getIsAdopt())) {
						summer += Integer.parseInt(cLog.getCrd());// 暑修主修學系
						majorPass += Integer.parseInt(cLog.getCrd());// 主修實得學系
						GradUtil.putResultMap(resultMap, cLog, cLog.getIsAdopt());
					} else if (aStudent.getGradMajorFaculty().equals(cLog.getFacultyCode()) && TO_SUMMER.equals(cLog.getGetManner())
							&& IS_ADOPT.equals(cLog.getIsAdopt())) {
						summerAdopt += Integer.parseInt(cLog.getCrd());// 暑修申請歸併    
						GradUtil.putResultMap(resultMap, cLog, cLog.getIsAdopt());
					} else if (!aStudent.getGradMajorFaculty().equals(cLog.getFacultyCode()) && TO_SUMMER.equals(cLog.getGetManner())) {
						summerOther += Integer.parseInt(cLog.getCrd());// 暑修其他選修
						GradUtil.putResultMap(resultMap, cLog, cLog.getIsAdopt());					
					} else if (aStudent.getGradMajorFaculty().equals(cLog.getFacultyCode()) && "3".equals(cLog.getGetManner())
							&& NOT_ADOPT.equals(cLog.getIsAdopt())) {
						popMajor += Integer.parseInt(cLog.getCrd());// 推廣主修學系
						GradUtil.putResultMap(resultMap, cLog, cLog.getIsAdopt());
					} else if (aStudent.getGradMajorFaculty().equals(cLog.getFacultyCode()) && "3".equals(cLog.getGetManner())
							&& IS_ADOPT.equals(cLog.getIsAdopt())) {
						popAdopt += Integer.parseInt(cLog.getCrd());// 推廣申請歸併
						GradUtil.putResultMap(resultMap, cLog, cLog.getIsAdopt());
					} else if (!aStudent.getGradMajorFaculty().equals(cLog.getFacultyCode()) && "3".equals(cLog.getGetManner())) {
						popOther += Integer.parseInt(cLog.getCrd());// 推廣其他選修
						GradUtil.putResultMap(resultMap, cLog, cLog.getIsAdopt());
					}
				}
				// 新增Log
				Hashtable hashtable = transformer.poToHashtable(cLog);
				grat030Dao = new GRAT030DAO(dbManager, conn, hashtable, session);
				grat030Dao.insert();
				//109ccs check
				if( !graPassCcs109Yn && "2".equals(cLog.getGetManner()) && cLog.getGetAyear().compareTo(ParameterUtil.GRA_CCS_109) >= 0){
					graPassCcs109Yn = true;
				}
			}
		}
		
		//畢業參數
		GraParameters graParameters = (GraParameters) this.graParamHt.get(GradUtil.getGradKey(aStudent.getGradKind(), aStudent.getEdubkgrdAbility()));
		
		// 補零
		GradUtil.setZeroResultMap(facultyCodeList, resultMap);

		// 暑修超過30學分則要以30學分計算(不含暑修不採計)
//		int summerAll = summer + summerAdopt + summerOther + summerCommon + summerCommonAdopt;// + summerWithoutAdopt;
//		if (summerAll > Integer.parseInt(parameters.getSummer())) {
//			summerAll = Integer.parseInt(parameters.getSummer());
//		}

		int total = adopt + major + summer + summerAdopt + summerOther + summerCommon + summerCommonAdopt + other + common + commonAdopt + popAdopt + popMajor + popCommonMajor + popCommonAdopt + popOther + remainCrd;
		//int totalForMulti = adopt + major + summerAll + other + common + commonAdopt + popAdopt + popMajor + popCommonMajor + popCommonAdopt + popOther + multiCrd;
		
		// 處理減修學分
		int reduce = 0;
		if (reducedCrdMap.get(aStudent.getStno()) != null) {
			reduce = Integer.parseInt((String) reducedCrdMap.get(aStudent.getStno()));
		}

		// check總學分
		if (total + reduce < Integer.parseInt(graParameters.getGraTotal())) {		
			errorMsg.append(Reasons.TOTAL_NOT_ENOUGH);
			errorMsgCode.append(Reasons.TOTAL_NOT_ENOUGH_CODE);
		}

		
		// check主修學系
		if ((adopt + major + summer + summerAdopt + popAdopt + popMajor) < Integer.parseInt(graParameters.getMajorTotal())) {
			errorMsg.append(Reasons.MAJOR_1_NOT_ENOUGH);
			errorMsgCode.append(Reasons.MAJOR_1_NOT_ENOUGH_CODE);

			if ("831181414".equals(aStudent.getStno()) || "851440283".equals(aStudent.getStno()) || "871292535".equals(aStudent.getStno())) {
				errorMsgCode.append(Reasons.MAJOR_1_NOT_ENOUGH_CODE_M);
			}
		}

		// check採計上限
		int pAdopt = GradUtil.getAdoptCrdParameter(adoptCrdList, aStudent, aStudent.getGradMajorFaculty());// 採計他系上限
		if (pAdopt != -1) {
			// if ((adopt + commonAdopt + summerAdopt + summerCommonAdopt +
			// popAdopt + popCommonAdopt) > pAdopt) {
			if ((adopt + summerAdopt + popAdopt) > pAdopt) {
				errorMsg.append(Reasons.ADOPT_OVER);
				errorMsgCode.append(Reasons.ADOPT_OVER_CODE);
			}
		}

		// check暑修上限
		if ((summer + summerAdopt + summerOther + summerCommon + summerCommonAdopt) > Integer.parseInt(parameters.getSummer())) {
			errorMsg.append(Reasons.SUMMER_OVER);
			errorMsgCode.append(Reasons.SUMMER_OVER_CODE);
		}

		// check推廣上限
		if ((popAdopt + popMajor + popCommonMajor + popCommonAdopt + popOther) > Integer.parseInt(parameters.getPop())) {
			errorMsg.append(Reasons.POP_OVER);
			errorMsgCode.append(Reasons.POP_OVER_CODE);
		}

		
		//check必修科目群組
		if(!checkObligatoryCrd(aStudent.getGradMajorFaculty(),CrsnoList.toString())){
			errorMsg.append(Reasons.REQUIRED_NOT_ENOUGH);
			errorMsgCode.append(Reasons.REQUIRED_NOT_ENOUGH_CODE);
		}
		
		//check共同課程		
		if ((common + commonAdopt + summerCommon + summerCommonAdopt + popCommonMajor + popCommonAdopt) < Integer.parseInt(graParameters.getCommon())) {
			errorMsg.append(Reasons.COMMON_NOT_ENOUGH);
			errorMsgCode.append(Reasons.COMMON_NOT_ENOUGH_CODE);
		}
		
		// check 通識
		String gelMsg = checkGelCrd(aStudent, this.gelVector, graParameters, CrsnoList.toString());
		if (!"".equals(gelMsg)) {
			dbManager.logger.append("no pass DISCIPLINE_CODE =" + gelMsg);
			errorMsg.append(Reasons.GENERAL_NOT_ENOUGH);
			errorMsgCode.append(Reasons.GENERAL_NOT_ENOUGH_CODE);
		}
		
		// 實得學分
		System.out.println("graPassCcs109Yn="+graPassCcs109Yn);
		System.out.println("getGraPassCcs109="+graParameters.getGraPassCcs109());
		System.out.println("passCrd="+passCrd);
		System.out.println("graParameters.getGraPass()="+graParameters.getGraPass());
		System.out.println("majorPass="+majorPass);
		System.out.println("graParameters.getMajorPass()="+graParameters.getMajorPass());
		if (passCrd < Integer.parseInt(graParameters.getGraPass())) {
			errorMsg.append(Reasons.PASS_CRD_ENOUGH);
			errorMsgCode.append(Reasons.PASS_CRD_ENOUGH_CODE);
		}
		
		// 實得學分 + 109 css
		if (graPassCcs109Yn && (passCrd < Integer.parseInt(graParameters.getGraPassCcs109()))) {
			
			errorMsg.append(Reasons.PASS_CRD_ENOUGH);
			errorMsgCode.append(Reasons.PASS_CRD_ENOUGH_CODE);
		}

		// 主修實得學分
		if (majorPass < Integer.parseInt(graParameters.getMajorPass())) {
			errorMsg.append(Reasons.MAJORPASS_CRD_ENOUGH);
			errorMsgCode.append(Reasons.PASS_CRD_ENOUGH_CODE);
		}
		
		// check是否符合畢業資格
		Hashtable ht = new Hashtable();
		if ("".equals(errorMsg.toString().trim()) && "".equals(errorMsgCode.toString().trim())) {
			errorMsgCode.append(Reasons.AGREE_AUDIT_CODE);// 同意複審
			if (FIRST_VERIFY.equals(status)) {
				ht.put("AUTO_AUDIT_STATUS", PASS);
				ht.put("AUTO_AUDIT_UNQUAL_CAUSE", "");
			} else if (LAST_VERIFY.equals(status)) {
				ht.put("GRAD_REEXAM_STATUS", PASS);
				ht.put("GRAD_REEXAM_UNQUAL_CAUSE", "");
				ht.put("GRADE_SCORE", DataGetter.getGrade(dbManager, conn, aStudent, SINGLE));// 成績
				ht.put("GRAD_PROVE_NUMBER_1", GradUtil.getSenquence(dbManager, conn, session, aStudent.getAyear(), aStudent.getSms(), "1"));// 畢業證書號
				ht.put("GRAD_DATE", GradUtil.generateGradMonth("2", aStudent.getAyear(), aStudent.getSms()));// 畢業年月				
			}
		} else {
			if (FIRST_VERIFY.equals(status)) {
				ht.put("AUTO_AUDIT_STATUS", NOT_PASS);
				ht.put("AUTO_AUDIT_UNQUAL_CAUSE", errorMsg.toString());
			} else if (LAST_VERIFY.equals(status)) {
				ht.put("GRAD_REEXAM_STATUS", NOT_PASS);
				ht.put("GRAD_REEXAM_UNQUAL_CAUSE", errorMsg.toString());
				ht.put("GRADE_SCORE", DataGetter.getGrade(dbManager, conn, aStudent, SINGLE));// 成績
				ht.put("GRAD_PROVE_NUMBER_1", "");// 畢業證書號
				ht.put("GRAD_DATE", "");// 畢業年月
			}
		}

		// 存放共同課程
		resultMap.put("90", new Integer((common + summerCommon + popCommonMajor)));
		resultMap.put("90" + ADOPT_NAME, new Integer((commonAdopt + popCommonAdopt + summerCommonAdopt)));

		// 將grat003的狀態改為通過或不通過，並加上不通過原因
		StringBuffer sql = new StringBuffer();
		sql.append("AYEAR = '" + aStudent.getAyear() + "' ");
		sql.append("AND SMS = '" + aStudent.getSms() + "' ");
		sql.append("AND STNO = '" + aStudent.getStno() + "' ");

		GRAT003DAO grat003Dao = new GRAT003DAO(dbManager, conn, ht, session);
		grat003Dao.update(sql.toString());
		//by poto
		ht = new Hashtable();
		ht.put("GRADE_SCORE", DataGetter.getGrade(dbManager, conn, aStudent, SINGLE));// 成績
		grat003Dao = new GRAT003DAO(dbManager, conn, ht, session);		
		grat003Dao.update(sql.toString());
		

		// 畢業電腦初、複審詳細資料(ONE)
		VerifyDetail vd = new VerifyDetail();
		vd.setAyear(aStudent.getAyear());
		vd.setSms(aStudent.getSms());
		vd.setStno(aStudent.getStno());
		vd.setKind(status);
		vd.setTotal(String.valueOf(total));
		vd.setPop(String.valueOf((popAdopt + popMajor + popCommonMajor + popCommonAdopt + popOther)));
		vd.setSummer(String.valueOf((summer + summerOther + summerCommon + summerAdopt + summerCommonAdopt)));
		vd.setSummerWithoutAdopt(String.valueOf(summerWithoutAdopt));
		vd.setReduce(String.valueOf(crdAdopt.getReduceCRD(dbManager, conn, aStudent.getAyear(), aStudent.getSms(), aStudent.getStno())));
		vd.setResult(errorMsgCode.toString());

		Hashtable hashtable = transformer.poToHashtable(vd);
		GRAT028DAO grat028Dao = new GRAT028DAO(dbManager, conn, hashtable, session);
		grat028Dao.insert();

		// 畢業電腦初、複審詳細資料_各學系學分與採計學分(MULTI)
		Iterator iter = resultMap.keySet().iterator();
		while (iter.hasNext()) {
			String key = (String) iter.next();

			if (key.indexOf(ADOPT_NAME) != -1) {
				continue;
			}

			VerifyFaculty vf = new VerifyFaculty();

			vf.setAyear(aStudent.getAyear());
			vf.setSms(aStudent.getSms());
			vf.setStno(aStudent.getStno());
			vf.setKind(status);
			vf.setFacultyCode(key);

			Integer fTotal = (Integer) resultMap.get(key);
			if (fTotal != null) {
				vf.setFacultyTotal(String.valueOf(fTotal.intValue()));
			} else {
				vf.setFacultyTotal("0");
			}

			Integer aTotal = (Integer) resultMap.get(key + ADOPT_NAME);
			if (aTotal != null) {
				vf.setAdoptTotal(String.valueOf(aTotal.intValue()));
			} else {
				vf.setAdoptTotal("0");
			}

			Hashtable hashtableVf = transformer.poToHashtable(vf);
			GRAT029DAO grat029Dao = new GRAT029DAO(dbManager, conn, hashtableVf, session);
			grat029Dao.insert();
		}

		// 畢業電腦初、複審詳細資料_共同科學類學分(MULTI)
		iter = commonMap.keySet().iterator();
		while (iter.hasNext()) {
			String key = (String) iter.next();

			VerifyCommon vc = new VerifyCommon();

			vc.setAyear(aStudent.getAyear());
			vc.setSms(aStudent.getSms());
			vc.setStno(aStudent.getStno());
			vc.setKinds(status);
			vc.setKind(COMMON_PRINT_4);
			vc.setGroupCode(key);

			Integer cTotal = (Integer) commonMap.get(key);
			if (cTotal != null) {
				vc.setTotalCrd(String.valueOf(cTotal.intValue()));
			} else {
				vc.setTotalCrd("0");
			}

			Hashtable hashtableVc = transformer.poToHashtable(vc);
			GRAT031DAO grat031Dao = new GRAT031DAO(dbManager, conn, hashtableVc, session);
			grat031Dao.insert();
		}
	}

	protected void processKind02(ApplyStudents aStudent, StringBuffer errorMsg, StringBuffer errorMsgCode, List logList, List mList, int summerWithoutAdopt, List studyingCrsnoList, String status) throws Exception {
		// 取得學生修得的所有科目
		List aCrsno = DataGetter.getAllCrsno(dbManager, conn, aStudent, false);
		int passCrd =0;		
		StringBuffer CrsnoList = new StringBuffer() ;
		if (aCrsno != null && aCrsno.size() > 0) {
			for (int j = 0; j < aCrsno.size(); j++) {
				CrsnoInfo ci = (CrsnoInfo) aCrsno.get(j);
				
				// 初審排掉當學年期的科目
				//by poto 初審需要當學期的抵免  所以不要排除   成績的布要進來就好(目前做到的是全部近來)
				//if (this.isThisYear(status, aStudent, ci.getAyear(), ci.getSms())) {
				//	continue;
				//}
				if ("1".equals(ci.getGetManner())) {
					passCrd += Integer.parseInt(ci.getCrd());
				}
				if ("3".equals(ci.getGetManner())) {
					ci.setGetManner("2");
				}

				//by poto 處理必修
				if(CrsnoList.length()!=0){					
					CrsnoList.append(","+ci.getCrsno());
				}else{					
					CrsnoList.append(ci.getCrsno());
				}

				// 判斷是否為暑修課程
				String key = ci.getAyear() + ci.getSms() + ci.getCrsno();
				// if (!IS_SUMMER.equals(DataGetter.isSummer(dbManager, conn,
				// aStudent.getStno(), ci.getCrsno()))) {
				if ("1".equals(ci.getSms()) || "2".equals(ci.getSms()) || "2".equals(ci.getGetManner()) || summerMap.get(key) != null) {
					// 記錄科目歸屬學系的LOG
					CheckingLog cLog = new CheckingLog();
					cLog.setAyear(aStudent.getAyear());// 申請學年
					cLog.setSms(aStudent.getSms());// 申請學期
					cLog.setStno(ci.getStno());
					cLog.setKind(status);
					cLog.setFacultyCode(ci.getFacultyCode());

//					String[] fCode = ci.getFacultyCode().split(",");
//					if (fCode.length == 1) {
//						cLog.setFacultyCode(ci.getFacultyCode());
//					} else if (fCode.length > 1) {
//						if (ci.getFacultyCode().indexOf("90") != -1) {
//							cLog.setFacultyCode("90");
//						} else if (ci.getFacultyCode().indexOf(aStudent.getDbmajorGradFacultyCode1()) != -1) {
//							cLog.setFacultyCode(aStudent.getDbmajorGradFacultyCode1());
//						} else if (ci.getFacultyCode().indexOf(aStudent.getDbmajorGradFacultyCode2()) != -1) {
//							cLog.setFacultyCode(aStudent.getDbmajorGradFacultyCode2());
//						} else {
//							cLog.setFacultyCode(fCode[0]);
//						}
//					}

					cLog.setCrsno(ci.getCrsno());
					cLog.setCrd(this.getCrsnoNewCrd(ci, cLog.getFacultyCode()));
					cLog.setGetAyear(ci.getAyear());// 科目取得學年
					cLog.setGetSms(ci.getSms());// 科目取得學期

					// 計算採認學分
					String ccst003Key = aStudent.getAyear() + aStudent.getSms() + aStudent.getStno() + ci.getCrsno();
					if (this.nouMap.get(ccst003Key) != null && "2".equals(ci.getGetManner())) {
						nouCrd += Integer.parseInt(ci.getCrd());
					}

					// 判斷是否為當學年期修的課(這邊不會有暑修的課程)
					if (status.equals(FIRST_VERIFY)) {
						// 當學年期的科目已在join時被排除掉了, 因學分銀行尚未有資料, log也不需要有
						// if (!GradUtil.isStudyingCrsno(studyingCrsnoList,
						// aStudent, ci)) {
						cLog.setGetManner(ci.getGetManner());
						// } else {
						// cLog.setGetManner(TO_THIS_YEAR);
						// }
					} else if (status.equals(LAST_VERIFY)) {
						cLog.setGetManner(ci.getGetManner());
					}

					// 判斷科目是否有效
					if (GradUtil.isCrsnoValid(deadlineList, cLog)) {
						cLog.setIsValid(IS_VALID);
					} else {
						cLog.setIsValid(NOT_VALID);
					}

					cLog.setIsAdopt(NOT_ADOPT);

					// 相同性質科目
					if (IS_VALID.equals(cLog.getIsValid())) {
						String tempKey = aStudent.getStno() + cLog.getGetAyear() + cLog.getGetSms() + cLog.getCrsno() + cLog.getGetManner();
						if (this.crsnoMap.get(tempKey) != null) {
							cLog.setIsValid(NOT_VALID);
						}
					}

					// 判斷科目是否為多科取部分科目
					MultiCrsno mc = GradUtil.isMultiCrsno(multiCrsnoList, aStudent, cLog);
					if (mc == null) {
						logList.add(cLog);
					} else {
						cLog.setMc(mc);
						mList.add(cLog);
						multiCrd += Integer.parseInt(cLog.getCrd());
					}
				} else {
					summerWithoutAdopt += Integer.parseInt(ci.getCrd());
				}
			}
		}

		// 取得學生暑修與採計的所有科目
		List adoptList = DataGetter.getAdoptCrsno(dbManager, conn, aStudent);
		if (adoptList != null && adoptList.size() > 0) {
			for (int j = 0; j < adoptList.size(); j++) {
				CrsnoAdopt ca = (CrsnoAdopt) adoptList.get(j);

				// 初審排掉當學年期的科目
				//by poto 初審需要當學期的抵免  所以不要排除   成績的布要進來就好(目前做到的是全部近來)
				//if (this.isThisYear(status, aStudent, ca.getGetAyear(), ca.getGetSms())) {
				//	continue;
				//}

				if ("3".equals(ca.getGetManner())) {
					ca.setGetManner("2");
				}
				
				if ("1".equals(ca.getGetManner())) {
					passCrd += Integer.parseInt(ca.getCrd());
				}

				if(CrsnoList.length()!=0){					
					CrsnoList.append(","+ca.getCrsno());
				}else{					
					CrsnoList.append(ca.getCrsno());
				}

				// 記錄科目歸屬學系的LOG
				CheckingLog cLog = new CheckingLog();
				cLog.setAyear(aStudent.getAyear());// 申請學年
				cLog.setSms(aStudent.getSms());// 申請學期
				cLog.setStno(ca.getStno());
				cLog.setKind(status);

				boolean isSummerMajor = false;
				if (ca.getFacultyCode() != null && !"".equals(ca.getFacultyCode().trim())) {
					cLog.setFacultyCode(ca.getFacultyCode());
					if (IS_SUMMER.equals(ca.getIsSummer())) {
						isSummerMajor = true;
					}
				} else {
					cLog.setFacultyCode(ca.getAdoptFaculty());
				}

				cLog.setCrsno(ca.getCrsno());
				cLog.setCrd(this.getCrsnoNewCrd(ca));
				cLog.setGetAyear(ca.getGetAyear());// 科目取得學年
				cLog.setGetSms(ca.getGetSms());// 科目取得學期

				// 計算採認學分
				String ccst003Key = aStudent.getAyear() + aStudent.getSms() + aStudent.getStno() + ca.getCrsno();
				if (this.nouMap.get(ccst003Key) != null && "2".equals(ca.getGetManner())) {
					nouCrd += Integer.parseInt(ca.getCrd());
				}

				// 判斷是否為當學年期修的課
				if (status.equals(FIRST_VERIFY)) {
					// 當學年期的科目已在join時被排除掉了, 因學分銀行尚未有資料, log也不需要有
					// if (!GradUtil.isStudyingCrsno(studyingCrsnoList,
					// aStudent, ca)) {
					if (IS_SUMMER.equals(ca.getIsSummer())) {
						cLog.setGetManner(TO_SUMMER);
					} else {
						cLog.setGetManner(ca.getGetManner());
					}
					// } else {
					// cLog.setGetManner(TO_THIS_YEAR);
					// }
				} else if (status.equals(LAST_VERIFY)) {
					if (IS_SUMMER.equals(ca.getIsSummer())) {
						cLog.setGetManner(TO_SUMMER);
					} else {
						cLog.setGetManner(ca.getGetManner());
					}
				}

				// 判斷科目是否有效
				if (GradUtil.isCrsnoValid(deadlineList, cLog)) {
					cLog.setIsValid(IS_VALID);
				} else {
					cLog.setIsValid(NOT_VALID);
				}

				// 判斷暑修課程是否為主修學系
				if (isSummerMajor) {
					cLog.setIsAdopt(NOT_ADOPT);
				} else {
					// 20090106新增, 兩系共開於cout103還是會設定成一主多採形式, 但書面上還是會記錄兩系皆為主開
					// 於Gra002m申請時若決定將該科採計於其他科時, 這邊會判斷兩系共開, 並將其改為主修學系
					if (this.CommonCrsnoMap.get(cLog.getGetAyear() + cLog.getGetSms() + cLog.getCrsno() + "_COMMON") != null) {
						String commonFaculty = (String) this.CommonCrsnoMap.get(cLog.getGetAyear() + cLog.getGetSms() + cLog.getCrsno() + "_COMMON");
						String planFaculty = (String) this.CommonCrsnoMap.get(cLog.getGetAyear() + cLog.getGetSms() + cLog.getCrsno() + "_PLAN");
						if (aStudent.getDbmajorGradFacultyCode1().equals(commonFaculty) || aStudent.getDbmajorGradFacultyCode2().equals(commonFaculty) || aStudent.getDbmajorGradFacultyCode1().equals(planFaculty) || aStudent.getDbmajorGradFacultyCode2().equals(planFaculty)) {
							cLog.setIsAdopt(NOT_ADOPT);
						} else {
							cLog.setIsAdopt(IS_ADOPT);
						}
					} else {
						cLog.setIsAdopt(IS_ADOPT);
					}
				}

				// 相同性質科目
				if (IS_VALID.equals(cLog.getIsValid())) {
					String tempKey = aStudent.getStno() + cLog.getGetAyear() + cLog.getGetSms() + cLog.getCrsno() + cLog.getGetManner();
					if (this.crsnoMap.get(tempKey) != null) {
						cLog.setIsValid(NOT_VALID);
					}
				}

				// 判斷科目是否為多科取部分科目
				MultiCrsno mc = GradUtil.isMultiCrsno(multiCrsnoList, aStudent, cLog);
				if (mc == null) {
					logList.add(cLog);
				} else {
					cLog.setMc(mc);
					mList.add(cLog);
					multiCrd += Integer.parseInt(cLog.getCrd());
				}
			}
		}

		// 將多科取部份科目或N學分的科目加到logList
		remainCrd += GradUtil.processMultiCrsno(mList, logList);

		// 開始計算學分
		int adopt1 = 0;// 申請歸併
		int adopt2 = 0;// 申請歸併
		int major1 = 0;// 主修學系
		int major2 = 0;// 主修學系
		int summer1 = 0;// 暑修主修學系
		int summer2 = 0;// 暑修主修學系
		int summerAdopt1 = 0;// 暑修申請歸併
		int summerAdopt2 = 0;// 暑修申請歸併
		int summerOther = 0;// 暑修其他選修
		int summerCommon = 0;// 暑修共同課程
		int summerCommonAdopt = 0;// 暑修共同課程
		int popAdopt1 = 0;// 推廣申請歸併
		int popAdopt2 = 0;// 推廣申請歸併
		int popMajor1 = 0;// 推廣主修學系
		int popMajor2 = 0;// 推廣主修學系
		int popCommonMajor = 0;// 推廣共同課程主修學系
		int popCommonAdopt = 0;// 推廣共同課程申請歸併
		int popOther = 0;// 推廣其他選修
		int other = 0;// 其他選修
		int common = 0;// 共同課程
		int commonAdopt = 0;// 共同課程申請歸併
		int getCrd1 = 0;// 修得的學分
		int getCrd2 = 0;// 修得的學分
		boolean graPassCcs109Yn =  false; //判斷是否有109的CCS
		Map resultMap = new HashMap();
		Map commonMap = new HashMap();

		// 刪除Log
		StringBuffer condition = new StringBuffer();
		condition.append("AYEAR = '" + aStudent.getAyear() + "' ");
		condition.append("AND SMS = '" + aStudent.getSms() + "' ");
		condition.append("AND STNO = '" + aStudent.getStno() + "' ");

		GRAT030DAO grat030Dao = new GRAT030DAO(dbManager, conn);
		grat030Dao.delete(condition.toString() + "AND KIND = '" + status + "' ");

		GRAT028DAO grat028Daod = new GRAT028DAO(dbManager, conn);
		grat028Daod.delete(condition.toString() + "AND KIND = '" + status + "' ");

		GRAT029DAO grat029Daod = new GRAT029DAO(dbManager, conn);
		grat029Daod.delete(condition.toString() + "AND KIND = '" + status + "' ");

		GRAT031DAO grat031Daod = new GRAT031DAO(dbManager, conn);
		grat031Daod.delete(condition.toString() + "AND KINDS = '" + status + "' ");

		if (logList != null && logList.size() > 0) {
			for (int j = 0; j < logList.size(); j++) {
				CheckingLog cLog = (CheckingLog) logList.get(j);

				// 科目有效時才計算學分
				if (IS_VALID.equals(cLog.getIsValid()) || IS_MULTI_CRSNO_NCRD.equals(cLog.getIsValid()) || IS_MULTI_CRSNO_PART.equals(cLog.getIsValid())) {
					// 初審時當學年期修的課一律排除
					// 當學年期的科目已在join時被排除掉了, 因學分銀行尚未有資料, log也不需要有
					// if (FIRST_VERIFY.equals(status)) {
					// if (TO_THIS_YEAR.equals(cLog.getGetManner())) {
					// continue;
					// }
					// }

					if ("90".equals(cLog.getFacultyCode()) && NOT_ADOPT.equals(cLog.getIsAdopt())
							&& ("1".equals(cLog.getGetManner()) || "2".equals(cLog.getGetManner()))) {
						common += Integer.parseInt(cLog.getCrd());// 共同課程
						GradUtil.putCommonMap(commonKind, commonMap, cLog, COMMON_PRINT_4);
					} else if ("90".equals(cLog.getFacultyCode()) && IS_ADOPT.equals(cLog.getIsAdopt())
							&& ("1".equals(cLog.getGetManner()) || "2".equals(cLog.getGetManner()))) {
						commonAdopt += Integer.parseInt(cLog.getCrd());// 共同課程申請歸併
						GradUtil.putCommonMap(commonKind, commonMap, cLog, COMMON_PRINT_4);
					} else if (aStudent.getDbmajorGradFacultyCode1().equals(cLog.getFacultyCode()) && NOT_ADOPT.equals(cLog.getIsAdopt())
							&& ("1".equals(cLog.getGetManner()) || "2".equals(cLog.getGetManner()))) {
						major1 += Integer.parseInt(cLog.getCrd());// 主修學系
						if ("1".equals(cLog.getGetManner())) {
							getCrd1 += Integer.parseInt(cLog.getCrd());
						} else if ("2".equals(cLog.getGetManner())) {
							String ccst003Key = aStudent.getAyear() + aStudent.getSms() + aStudent.getStno() + cLog.getCrsno();
							if (this.nouMap.get(ccst003Key) != null) {
								getCrd1 += Integer.parseInt(cLog.getCrd());
							}
						}
						GradUtil.putResultMap(resultMap, cLog, cLog.getIsAdopt());
					} else if (aStudent.getDbmajorGradFacultyCode2().equals(cLog.getFacultyCode()) && NOT_ADOPT.equals(cLog.getIsAdopt())
							&& ("1".equals(cLog.getGetManner()) || "2".equals(cLog.getGetManner()))) {
						major2 += Integer.parseInt(cLog.getCrd());// 主修學系
						if ("1".equals(cLog.getGetManner())) {
							getCrd2 += Integer.parseInt(cLog.getCrd());
						} else if ("2".equals(cLog.getGetManner())) {
							String ccst003Key = aStudent.getAyear() + aStudent.getSms() + aStudent.getStno() + cLog.getCrsno();
							if (this.nouMap.get(ccst003Key) != null) {
								getCrd2 += Integer.parseInt(cLog.getCrd());
							}
						}
						GradUtil.putResultMap(resultMap, cLog, cLog.getIsAdopt());
					} else if (aStudent.getDbmajorGradFacultyCode1().equals(cLog.getFacultyCode()) && IS_ADOPT.equals(cLog.getIsAdopt())
							&& ("1".equals(cLog.getGetManner()) || "2".equals(cLog.getGetManner()))) {
						adopt1 += Integer.parseInt(cLog.getCrd());// 申請歸併
						if ("1".equals(cLog.getGetManner())) {
							getCrd1 += Integer.parseInt(cLog.getCrd());
						} else if ("2".equals(cLog.getGetManner())) {
							String ccst003Key = aStudent.getAyear() + aStudent.getSms() + aStudent.getStno() + cLog.getCrsno();
							if (this.nouMap.get(ccst003Key) != null) {
								getCrd1 += Integer.parseInt(cLog.getCrd());
							}
						}
						GradUtil.putResultMap(resultMap, cLog, cLog.getIsAdopt());
					} else if (aStudent.getDbmajorGradFacultyCode2().equals(cLog.getFacultyCode()) && IS_ADOPT.equals(cLog.getIsAdopt())
							&& ("1".equals(cLog.getGetManner()) || "2".equals(cLog.getGetManner()))) {
						adopt2 += Integer.parseInt(cLog.getCrd());// 申請歸併
						if ("1".equals(cLog.getGetManner())) {
							getCrd2 += Integer.parseInt(cLog.getCrd());
						} else if ("2".equals(cLog.getGetManner())) {
							String ccst003Key = aStudent.getAyear() + aStudent.getSms() + aStudent.getStno() + cLog.getCrsno();
							if (this.nouMap.get(ccst003Key) != null) {
								getCrd2 += Integer.parseInt(cLog.getCrd());
							}
						}
						GradUtil.putResultMap(resultMap, cLog, cLog.getIsAdopt());
					} else if (!aStudent.getDbmajorGradFacultyCode1().equals(cLog.getFacultyCode())
							&& !aStudent.getDbmajorGradFacultyCode2().equals(cLog.getFacultyCode())
							&& ("1".equals(cLog.getGetManner()) || "2".equals(cLog.getGetManner()))) {
						other += Integer.parseInt(cLog.getCrd());// 其他選修
						GradUtil.putResultMap(resultMap, cLog, cLog.getIsAdopt());
					} else if ("90".equals(cLog.getFacultyCode()) && TO_SUMMER.equals(cLog.getGetManner()) && NOT_ADOPT.equals(cLog.getIsAdopt())) {
						summerCommon += Integer.parseInt(cLog.getCrd());// 暑修共同課程
						GradUtil.putCommonMap(commonKind, commonMap, cLog, COMMON_PRINT_4);
					} else if ("90".equals(cLog.getFacultyCode()) && TO_SUMMER.equals(cLog.getGetManner()) && IS_ADOPT.equals(cLog.getIsAdopt())) {
						summerCommonAdopt += Integer.parseInt(cLog.getCrd());// 暑修共同課程
						GradUtil.putCommonMap(commonKind, commonMap, cLog, COMMON_PRINT_4);
					} else if (aStudent.getDbmajorGradFacultyCode1().equals(cLog.getFacultyCode()) && TO_SUMMER.equals(cLog.getGetManner())
							&& NOT_ADOPT.equals(cLog.getIsAdopt())) {
						summer1 += Integer.parseInt(cLog.getCrd());// 暑修主修學系
						getCrd1 += Integer.parseInt(cLog.getCrd());
						GradUtil.putResultMap(resultMap, cLog, cLog.getIsAdopt());
					} else if (aStudent.getDbmajorGradFacultyCode2().equals(cLog.getFacultyCode()) && TO_SUMMER.equals(cLog.getGetManner())
							&& NOT_ADOPT.equals(cLog.getIsAdopt())) {
						summer2 += Integer.parseInt(cLog.getCrd());// 暑修主修學系
						getCrd2 += Integer.parseInt(cLog.getCrd());
						GradUtil.putResultMap(resultMap, cLog, cLog.getIsAdopt());
					} else if (aStudent.getDbmajorGradFacultyCode1().equals(cLog.getFacultyCode()) && TO_SUMMER.equals(cLog.getGetManner())
							&& IS_ADOPT.equals(cLog.getIsAdopt())) {
						summerAdopt1 += Integer.parseInt(cLog.getCrd());// 暑修申請歸併
						getCrd1 += Integer.parseInt(cLog.getCrd());
						GradUtil.putResultMap(resultMap, cLog, cLog.getIsAdopt());
					} else if (aStudent.getDbmajorGradFacultyCode2().equals(cLog.getFacultyCode()) && TO_SUMMER.equals(cLog.getGetManner())
							&& IS_ADOPT.equals(cLog.getIsAdopt())) {
						summerAdopt2 += Integer.parseInt(cLog.getCrd());// 暑修申請歸併
						getCrd2 += Integer.parseInt(cLog.getCrd());
						GradUtil.putResultMap(resultMap, cLog, cLog.getIsAdopt());
					} else if (!aStudent.getDbmajorGradFacultyCode1().equals(cLog.getFacultyCode())
							&& !aStudent.getDbmajorGradFacultyCode2().equals(cLog.getFacultyCode()) && TO_SUMMER.equals(cLog.getGetManner())) {
						summerOther += Integer.parseInt(cLog.getCrd());// 暑修其他選修
						GradUtil.putResultMap(resultMap, cLog, cLog.getIsAdopt());
					} else if ("90".equals(cLog.getFacultyCode()) && "3".equals(cLog.getGetManner()) && NOT_ADOPT.equals(cLog.getIsAdopt())) {
						popCommonMajor += Integer.parseInt(cLog.getCrd());// 推廣共同課程主修學系
						GradUtil.putCommonMap(commonKind, commonMap, cLog, COMMON_PRINT_4);
					} else if ("90".equals(cLog.getFacultyCode()) && "3".equals(cLog.getGetManner()) && IS_ADOPT.equals(cLog.getIsAdopt())) {
						popCommonAdopt += Integer.parseInt(cLog.getCrd());// 推廣共同課程申請歸併
						GradUtil.putCommonMap(commonKind, commonMap, cLog, COMMON_PRINT_4);
					} else if (aStudent.getDbmajorGradFacultyCode1().equals(cLog.getFacultyCode()) && "3".equals(cLog.getGetManner())
							&& NOT_ADOPT.equals(cLog.getIsAdopt())) {
						popMajor1 += Integer.parseInt(cLog.getCrd());// 推廣主修學系
						GradUtil.putResultMap(resultMap, cLog, cLog.getIsAdopt());
					} else if (aStudent.getDbmajorGradFacultyCode2().equals(cLog.getFacultyCode()) && "3".equals(cLog.getGetManner())
							&& NOT_ADOPT.equals(cLog.getIsAdopt())) {
						popMajor2 += Integer.parseInt(cLog.getCrd());// 推廣主修學系
						GradUtil.putResultMap(resultMap, cLog, cLog.getIsAdopt());
					} else if (aStudent.getDbmajorGradFacultyCode1().equals(cLog.getFacultyCode()) && "3".equals(cLog.getGetManner())
							&& IS_ADOPT.equals(cLog.getIsAdopt())) {
						popAdopt1 += Integer.parseInt(cLog.getCrd());// 推廣申請歸併
						GradUtil.putResultMap(resultMap, cLog, cLog.getIsAdopt());
					} else if (aStudent.getDbmajorGradFacultyCode2().equals(cLog.getFacultyCode()) && "3".equals(cLog.getGetManner())
							&& IS_ADOPT.equals(cLog.getIsAdopt())) {
						popAdopt2 += Integer.parseInt(cLog.getCrd());// 推廣申請歸併
						GradUtil.putResultMap(resultMap, cLog, cLog.getIsAdopt());
					} else if (!aStudent.getDbmajorGradFacultyCode1().equals(cLog.getFacultyCode())
							&& !aStudent.getDbmajorGradFacultyCode2().equals(cLog.getFacultyCode()) && "3".equals(cLog.getGetManner())) {
						popOther += Integer.parseInt(cLog.getCrd());// 推廣其他選修
						GradUtil.putResultMap(resultMap, cLog, cLog.getIsAdopt());
					}
				}

				// 新增Log
				Hashtable hashtable = transformer.poToHashtable(cLog);
				grat030Dao = new GRAT030DAO(dbManager, conn, hashtable, session);
				grat030Dao.insert();
				//109ccs check
				if( !graPassCcs109Yn && "2".equals(cLog.getGetManner()) && cLog.getGetAyear().compareTo(ParameterUtil.GRA_CCS_109) >= 0 ){
					graPassCcs109Yn = true;
				}
			}
		}

		// 補零
		GradUtil.setZeroResultMap(facultyCodeList, resultMap);

		
		//畢業參數
		GraParameters graParameters = (GraParameters) this.graParamHt.get(GradUtil.getGradKey(aStudent.getGradKind(), aStudent.getEdubkgrdAbility()));
		
		// 暑修超過30學分則要以30學分計算(含暑修不採計)
//		int summerAll = summer1 + summer2 + summerAdopt1 + summerAdopt2 + summerOther + summerCommon + summerCommonAdopt + summerWithoutAdopt;
//		if (summerAll > Integer.parseInt(parameters.getDoubleSummer())) {
//			summerAll = Integer.parseInt(parameters.getDoubleSummer());
//		}

		int total = adopt1 + adopt2 + major1 + major2 + summer1 + summer2 + summerAdopt1 + summerAdopt2 + summerOther + summerCommon + summerCommonAdopt
				+ other + common + commonAdopt + popAdopt1 + popAdopt2 + popMajor1 + popMajor2 + popCommonMajor + popCommonAdopt + popOther + remainCrd;

		// 處理減修學分
		int reduce = 0;
		if (reducedCrdMap.get(aStudent.getStno()) != null) {
			reduce = Integer.parseInt((String) reducedCrdMap.get(aStudent.getStno()));
		}

		// check總學分
		if (total + reduce < Integer.parseInt(parameters.getDoubleTotal())) {
			errorMsg.append(Reasons.TOTAL_NOT_ENOUGH);
			errorMsgCode.append(Reasons.TOTAL_NOT_ENOUGH_CODE);
		}		

		// check主修學系一
		if ((adopt1 + major1 + popAdopt1 + popMajor1 + summer1 + summerAdopt1) < Integer.parseInt(parameters.getDoubleMajor1())) {
			errorMsg.append(Reasons.MAJOR_1_NOT_ENOUGH);
			errorMsgCode.append(Reasons.MAJOR_1_NOT_ENOUGH_CODE);

//			if (multiCrd > 0) {
//				errorMsgCode.append(Reasons.MAJOR_1_NOT_ENOUGH_CODE_M);
//			}
		}

		// check主修學系二
		if ((adopt2 + major2 + popAdopt2 + popMajor2 + summer2 + summerAdopt2) < Integer.parseInt(parameters.getDoubleMajor2())) {
			errorMsg.append(Reasons.MAJOR_2_NOT_ENOUGH);
			errorMsgCode.append(Reasons.MAJOR_2_NOT_ENOUGH_CODE);

//			if (multiCrd > 0) {
//				errorMsgCode.append(Reasons.MAJOR_2_NOT_ENOUGH_CODE_N);
//			}
		}

		// check採計上限(共同課程除外)
		int pAdopt1 = GradUtil.getAdoptCrdParameter(adoptCrdList, aStudent, aStudent.getDbmajorGradFacultyCode1());// 採計他系上限
		if (pAdopt1 != -1) {
			if ((adopt1 + popAdopt1 + summerAdopt1) > pAdopt1) {
				errorMsg.append(Reasons.ADOPT_OVER);
				errorMsgCode.append(Reasons.ADOPT_OVER_CODE);
			}
		}

		int pAdopt2 = GradUtil.getAdoptCrdParameter(adoptCrdList, aStudent, aStudent.getDbmajorGradFacultyCode2());// 採計他系上限
		if (pAdopt2 != -1) {
			if ((adopt2 + popAdopt2 + summerAdopt2) > pAdopt2) {
				errorMsg.append(Reasons.ADOPT_OVER);
				errorMsgCode.append(Reasons.ADOPT_OVER_CODE);
			}
		}

		// check暑修上限
		if ((summer1 + summer2 + summerOther + summerCommon + summerAdopt1 + summerAdopt2) > Integer.parseInt(parameters.getDoubleSummer())) {
			errorMsg.append(Reasons.SUMMER_OVER);
			errorMsgCode.append(Reasons.SUMMER_OVER_CODE);
		}

		// check推廣上限
		if ((popAdopt1 + popAdopt2 + popMajor1 + popMajor2 + popCommonMajor + popCommonAdopt + popOther) > Integer.parseInt(parameters.getDoublePop())) {
			errorMsg.append(Reasons.POP_OVER);
			errorMsgCode.append(Reasons.POP_OVER_CODE);
		}

		// check主修學系一本校修得
		if (getCrd1 < Integer.parseInt(parameters.getDoubleOpen1())) {
			errorMsg.append(Reasons.CODE_1_NOT_ENOUGH);
			errorMsgCode.append(Reasons.CODE_1_NOT_ENOUGH_CODE);
		}

		// check主修學系二本校修得
		if (getCrd2 < Integer.parseInt(parameters.getDoubleOpen2())) {
			errorMsg.append(Reasons.CODE_2_NOT_ENOUGH);
			errorMsgCode.append(Reasons.CODE_2_NOT_ENOUGH_CODE);
		}

		// 處理必修科目		
		//if ("40".equals(aStudent.getDbmajorGradFacultyCode1()) || "40".equals(aStudent.getDbmajorGradFacultyCode2())) {
		//	if (old < 5 && (new1 < 3 || new2 < 3)) {
		//		// 不符合, 記錄不通過原因
		//		errorMsg.append(Reasons.REQUIRED_NOT_ENOUGH);
		//		errorMsgCode.append(Reasons.REQUIRED_NOT_ENOUGH_CODE);
		//	}
		//}
		//by poto 處理必修
		if(!checkObligatoryCrd(aStudent.getDbmajorGradFacultyCode1(),CrsnoList.toString())){
			if(!checkObligatoryCrd(aStudent.getDbmajorGradFacultyCode2(),CrsnoList.toString())){				
				errorMsg.append(Reasons.REQUIRED_NOT_ENOUGH);
				errorMsgCode.append(Reasons.REQUIRED_NOT_ENOUGH_CODE);
			}
		}
		
		//check共同課程		
		if ((common + commonAdopt + summerCommon + summerCommonAdopt + popCommonMajor + popCommonAdopt) < Integer.parseInt(graParameters.getCommon())) {
			errorMsg.append(Reasons.COMMON_NOT_ENOUGH);
			errorMsgCode.append(Reasons.COMMON_NOT_ENOUGH_CODE);
		}

		// check 通識
		String gelMsg = checkGelCrd(aStudent, this.gelVector, graParameters, CrsnoList.toString());
		if (!"".equals(gelMsg)) {
			dbManager.logger.append("no pass DISCIPLINE_CODE =" + gelMsg);
			errorMsg.append(Reasons.GENERAL_NOT_ENOUGH);
			errorMsgCode.append(Reasons.GENERAL_NOT_ENOUGH_CODE);
		}
		
		// 實得學分
		if(passCrd<Integer.parseInt(this.parameters.getPassCrd())){
			errorMsg.append(Reasons.PASS_CRD_ENOUGH);
			errorMsgCode.append(Reasons.PASS_CRD_ENOUGH_CODE);
		}
		
		// 實得學分 + 109 css
		if (graPassCcs109Yn && (passCrd < Integer.parseInt(graParameters.getGraPassCcs109()))) {
			errorMsg.append(Reasons.PASS_CRD_ENOUGH);
			errorMsgCode.append(Reasons.PASS_CRD_ENOUGH_CODE);
		}

		// check是否符合畢業資格
		Hashtable ht = new Hashtable();
		if ("".equals(errorMsg.toString().trim()) && "".equals(errorMsgCode.toString().trim())) {
			errorMsgCode.append(Reasons.AGREE_AUDIT_CODE);// 同意複審
			if (FIRST_VERIFY.equals(status)) {
				ht.put("AUTO_AUDIT_STATUS", PASS);
				ht.put("AUTO_AUDIT_UNQUAL_CAUSE", "");
			} else if (LAST_VERIFY.equals(status)) {
				ht.put("GRAD_REEXAM_STATUS", PASS);
				ht.put("GRAD_REEXAM_UNQUAL_CAUSE", "");
				ht.put("GRADE_SCORE", DataGetter.getGrade(dbManager, conn, aStudent, SINGLE));// 成績
				ht.put("GRAD_PROVE_NUMBER_1", GradUtil.getSenquence(dbManager, conn, session, aStudent.getAyear(), aStudent.getSms(), "1"));// 畢業證書號
				ht.put("GRAD_DATE", GradUtil.generateGradMonth("2", aStudent.getAyear(), aStudent.getSms()));// 畢業年月
			}
		} else {
			if (FIRST_VERIFY.equals(status)) {
				ht.put("AUTO_AUDIT_STATUS", NOT_PASS);
				ht.put("AUTO_AUDIT_UNQUAL_CAUSE", errorMsg.toString());
			} else if (LAST_VERIFY.equals(status)) {
				ht.put("GRAD_REEXAM_STATUS", NOT_PASS);
				ht.put("GRAD_REEXAM_UNQUAL_CAUSE", errorMsg.toString());
				ht.put("GRADE_SCORE", DataGetter.getGrade(dbManager, conn, aStudent, SINGLE));// 成績
				ht.put("GRAD_PROVE_NUMBER_1", "");// 畢業證書號
				ht.put("GRAD_DATE", "");// 畢業年月
			}
		}
		
		
		// 存放共同課程
		resultMap.put("90", new Integer((common + summerCommon + popCommonMajor)));
		resultMap.put("90" + ADOPT_NAME, new Integer((commonAdopt + popCommonAdopt + summerCommonAdopt)));

		// 將grat003的狀態改為通過或不通過，並加上不通過原因
		StringBuffer sql = new StringBuffer();
		sql.append("AYEAR = '" + aStudent.getAyear() + "' ");
		sql.append("AND SMS = '" + aStudent.getSms() + "' ");
		sql.append("AND STNO = '" + aStudent.getStno() + "' ");

		GRAT003DAO grat003Dao = new GRAT003DAO(dbManager, conn, ht, session);
		grat003Dao.update(sql.toString());
		
		//by poto
		ht = new Hashtable();
		ht.put("GRADE_SCORE", DataGetter.getGrade(dbManager, conn, aStudent, SINGLE));// 成績
		grat003Dao = new GRAT003DAO(dbManager, conn, ht, session);		
		grat003Dao.update(sql.toString());

		// 畢業電腦初、複審詳細資料(ONE)
		VerifyDetail vd = new VerifyDetail();
		vd.setAyear(aStudent.getAyear());
		vd.setSms(aStudent.getSms());
		vd.setStno(aStudent.getStno());
		vd.setKind(status);
		vd.setTotal(String.valueOf(total));
		vd.setPop(String.valueOf((popAdopt1 + popAdopt2 + popMajor1 + popMajor2 + popCommonMajor + popCommonAdopt + popOther)));
		vd.setSummer(String.valueOf((summer1 + summer2 + summerOther + summerCommon + summerAdopt1 + summerAdopt2)));
		vd.setSummerWithoutAdopt(String.valueOf(summerWithoutAdopt));
		vd.setReduce(String.valueOf(crdAdopt.getReduceCRD(dbManager, conn, aStudent.getAyear(), aStudent.getSms(), aStudent.getStno())));
		vd.setResult(errorMsgCode.toString());

		Hashtable hashtable = transformer.poToHashtable(vd);
		GRAT028DAO grat028Dao = new GRAT028DAO(dbManager, conn, hashtable, session);
		grat028Dao.insert();

		// 畢業電腦初、複審詳細資料_各學系學分與採計學分(MULTI)
		Iterator iter = resultMap.keySet().iterator();
		while (iter.hasNext()) {
			String key = (String) iter.next();

			if (key.indexOf(ADOPT_NAME) != -1) {
				continue;
			}

			VerifyFaculty vf = new VerifyFaculty();

			vf.setAyear(aStudent.getAyear());
			vf.setSms(aStudent.getSms());
			vf.setStno(aStudent.getStno());
			vf.setKind(status);
			vf.setFacultyCode(key);

			Integer fTotal = (Integer) resultMap.get(key);
			if (fTotal != null) {
				vf.setFacultyTotal(String.valueOf(fTotal.intValue()));
			} else {
				vf.setFacultyTotal("0");
			}

			Integer aTotal = (Integer) resultMap.get(key + ADOPT_NAME);
			if (aTotal != null) {
				vf.setAdoptTotal(String.valueOf(aTotal.intValue()));
			} else {
				vf.setAdoptTotal("0");
			}

			Hashtable hashtableVf = transformer.poToHashtable(vf);
			GRAT029DAO grat029Dao = new GRAT029DAO(dbManager, conn, hashtableVf, session);
			grat029Dao.insert();
		}

		// 畢業電腦初、複審詳細資料_共同科學類學分(MULTI)
		iter = commonMap.keySet().iterator();
		while (iter.hasNext()) {
			String key = (String) iter.next();

			VerifyCommon vc = new VerifyCommon();

			vc.setAyear(aStudent.getAyear());
			vc.setSms(aStudent.getSms());
			vc.setStno(aStudent.getStno());
			vc.setKinds(status);
			vc.setKind(COMMON_PRINT_4);
			vc.setGroupCode(key);

			Integer cTotal = (Integer) commonMap.get(key);
			if (cTotal != null) {
				vc.setTotalCrd(String.valueOf(cTotal.intValue()));
			} else {
				vc.setTotalCrd("0");
			}

			Hashtable hashtableVc = transformer.poToHashtable(vc);
			GRAT031DAO grat031Dao = new GRAT031DAO(dbManager, conn, hashtableVc, session);
			grat031Dao.insert();
		}
	}

	protected void processKind05(ApplyStudents aStudent, StringBuffer errorMsg, StringBuffer errorMsgCode, List logList, List mList, int summerWithoutAdopt, List studyingCrsnoList, String status) throws Exception {
		// 取得學生修得的所有科目
		List aCrsno = DataGetter.getAllCrsno(dbManager, conn, aStudent, false);
		int passCrd =0;
		StringBuffer CrsnoList = new StringBuffer() ;
		if (aCrsno != null && aCrsno.size() > 0) {
			for (int j = 0; j < aCrsno.size(); j++) {
				CrsnoInfo ci = (CrsnoInfo) aCrsno.get(j);
				if ("1".equals(ci.getGetManner())) {
					passCrd += Integer.parseInt(ci.getCrd());
				}
				if ("3".equals(ci.getGetManner())) {
					ci.setGetManner("2");
				}

				//by poto 處理必修
				if(CrsnoList.length()!=0){					
					CrsnoList.append(","+ci.getCrsno());
				}else{					
					CrsnoList.append(ci.getCrsno());
				}
				
				// 判斷是否為暑修課程
				String key = ci.getAyear() + ci.getSms() + ci.getCrsno();
				// if (!IS_SUMMER.equals(DataGetter.isSummer(dbManager, conn,
				// aStudent.getStno(), ci.getCrsno()))) {
				if ("1".equals(ci.getSms()) || "2".equals(ci.getSms()) || "2".equals(ci.getGetManner()) || summerMap.get(key) != null) {
					// 記錄科目歸屬學系的LOG
					CheckingLog cLog = new CheckingLog();
					cLog.setAyear(aStudent.getAyear());// 申請學年
					cLog.setSms(aStudent.getSms());// 申請學期
					cLog.setStno(ci.getStno());
					cLog.setKind(status);
					cLog.setFacultyCode(ci.getFacultyCode());
					cLog.setCrsno(ci.getCrsno());
					cLog.setCrd(this.getCrsnoNewCrd(ci, cLog.getFacultyCode()));
					cLog.setGetAyear(ci.getAyear());// 科目取得學年
					cLog.setGetSms(ci.getSms());// 科目取得學期

					// 判斷是否為當學年期修的課(這邊不會有暑修的課程)
					if (status.equals(FIRST_VERIFY)) {
						cLog.setGetManner(ci.getGetManner());
					} else if (status.equals(LAST_VERIFY)) {
						cLog.setGetManner(ci.getGetManner());
					}

					if (GradUtil.isCrsnoValid(deadlineList, cLog)) {
						cLog.setIsValid(IS_VALID);
					} else {
						cLog.setIsValid(NOT_VALID);
					}

					cLog.setIsAdopt(NOT_ADOPT);

					// 相同性質科目
					if (IS_VALID.equals(cLog.getIsValid())) {
						String tempKey = aStudent.getStno() + cLog.getGetAyear() + cLog.getGetSms() + cLog.getCrsno() + cLog.getGetManner();
						if (this.crsnoMap.get(tempKey) != null) {
							cLog.setIsValid(NOT_VALID);
						}
					}

					// 判斷科目是否為多科取部分科目
					MultiCrsno mc = GradUtil.isMultiCrsno(multiCrsnoList, aStudent, cLog);

					if (mc == null) {
						logList.add(cLog);
					} else {
						cLog.setMc(mc);
						mList.add(cLog);
						multiCrd += Integer.parseInt(cLog.getCrd());
					}
				} else {
					summerWithoutAdopt += Integer.parseInt(ci.getCrd());
				}
			}
		}

		// 取得學生暑修與採計的所有科目
		List adoptList = DataGetter.getAdoptCrsno(dbManager, conn, aStudent);
		if (adoptList != null && adoptList.size() > 0) {
			for (int j = 0; j < adoptList.size(); j++) {
				CrsnoAdopt ca = (CrsnoAdopt) adoptList.get(j);
				if ("3".equals(ca.getGetManner())) {
					ca.setGetManner("2");
				}

				if ("1".equals(ca.getGetManner())) {
					passCrd += Integer.parseInt(ca.getCrd());
				}

				if(CrsnoList.length()!=0){					
					CrsnoList.append(","+ca.getCrsno());
				}else{					
					CrsnoList.append(ca.getCrsno());
				}

				// 記錄科目歸屬學系的LOG
				CheckingLog cLog = new CheckingLog();
				cLog.setAyear(aStudent.getAyear());// 申請學年
				cLog.setSms(aStudent.getSms());// 申請學期
				cLog.setStno(ca.getStno());
				cLog.setKind(status);

				boolean isSummerMajor = false;
				if (ca.getFacultyCode() != null && !"".equals(ca.getFacultyCode().trim())) {
					cLog.setFacultyCode(ca.getFacultyCode());
					if (IS_SUMMER.equals(ca.getIsSummer())) {
						isSummerMajor = true;
					}
				} else {
					cLog.setFacultyCode(ca.getAdoptFaculty());
				}

				cLog.setCrsno(ca.getCrsno());
				cLog.setCrd(this.getCrsnoNewCrd(ca));
				cLog.setGetAyear(ca.getGetAyear());// 科目取得學年
				cLog.setGetSms(ca.getGetSms());// 科目取得學期

				// 判斷是否為當學年期修的課
				if (status.equals(FIRST_VERIFY)) {
					if (IS_SUMMER.equals(ca.getIsSummer())) {
						cLog.setGetManner(TO_SUMMER);
					} else {
						cLog.setGetManner(ca.getGetManner());
					}
				} else if (status.equals(LAST_VERIFY)) {
					if (IS_SUMMER.equals(ca.getIsSummer())) {
						cLog.setGetManner(TO_SUMMER);
					} else {
						cLog.setGetManner(ca.getGetManner());
					}
				}

				// 判斷科目是否有效
				if (GradUtil.isCrsnoValid(deadlineList, cLog)) {
					cLog.setIsValid(IS_VALID);
				} else {
					cLog.setIsValid(NOT_VALID);
				}

				// 判斷暑修課程是否為主修學系
				if (isSummerMajor) {
					cLog.setIsAdopt(NOT_ADOPT);
				} else {
					// 20090106新增, 兩系共開於cout103還是會設定成一主多採形式, 但書面上還是會記錄兩系皆為主開
					// 於Gra002m申請時若決定將該科採計於其他科時, 這邊會判斷兩系共開, 並將其改為主修學系
					if (this.CommonCrsnoMap.get(cLog.getGetAyear() + cLog.getGetSms() + cLog.getCrsno() + "_COMMON") != null) {
						String commonFaculty = (String) this.CommonCrsnoMap.get(cLog.getGetAyear() + cLog.getGetSms() + cLog.getCrsno() + "_COMMON");
						String planFaculty = (String) this.CommonCrsnoMap.get(cLog.getGetAyear() + cLog.getGetSms() + cLog.getCrsno() + "_PLAN");
						if (aStudent.getGradMajorFaculty().equals(commonFaculty) || aStudent.getGradMajorFaculty().equals(planFaculty)) {
							cLog.setIsAdopt(NOT_ADOPT);
						} else {
							cLog.setIsAdopt(IS_ADOPT);
						}
					} else {
						cLog.setIsAdopt(IS_ADOPT);
					}
				}

				// 相同性質科目
				if (IS_VALID.equals(cLog.getIsValid())) {
					String tempKey = aStudent.getStno() + cLog.getGetAyear() + cLog.getGetSms() + cLog.getCrsno() + cLog.getGetManner();
					if (this.crsnoMap.get(tempKey) != null) {
						cLog.setIsValid(NOT_VALID);
					}
				}

				// 判斷科目是否為多科取部分科目
				MultiCrsno mc = GradUtil.isMultiCrsno(multiCrsnoList, aStudent, cLog);
				if (mc == null) {
					logList.add(cLog);
				} else {
					cLog.setMc(mc);
					mList.add(cLog);
				}
			}
		}

		// 將多科取部份科目或N學分的科目加到logList
		remainCrd += GradUtil.processMultiCrsno(mList, logList);

		// 開始計算學分
		int adopt = 0;// 申請歸併
		int major = 0;// 主修學系
		int summer = 0;// 暑修主修學系
		int summerAdopt = 0;// 暑修申請歸併
		int summerOther = 0;// 暑修其他選修
		int summerCommon = 0;// 暑修共同課程
		int summerCommonAdopt = 0;// 暑修共同課程申請歸併
		int popAdopt = 0;// 推廣申請歸併
		int popMajor = 0;// 推廣主修學系
		int popCommonMajor = 0;// 推廣共同課程主修學系
		int popCommonAdopt = 0;// 推廣共同課程申請歸併
		int popOther = 0;// 推廣其他選修
		int other = 0;// 其他選修
		int common = 0;// 共同課程
		int commonAdopt = 0;// 共同課程申請歸併
		Map resultMap = new HashMap();
		Map commonMap = new HashMap();

		// 20090220新增
		int getCrd1 = 0;// 主修學系一本校修得
		int getCrd2 = 0;// 主修學系二本校修得

		// 刪除Log
		StringBuffer condition = new StringBuffer();
		condition.append("AYEAR = '" + aStudent.getAyear() + "' ");
		condition.append("AND SMS = '" + aStudent.getSms() + "' ");
		condition.append("AND STNO = '" + aStudent.getStno() + "' ");

		GRAT030DAO grat030Dao = new GRAT030DAO(dbManager, conn);
		grat030Dao.delete(condition.toString() + "AND KIND = '" + status + "' ");

		GRAT028DAO grat028Daod = new GRAT028DAO(dbManager, conn);
		grat028Daod.delete(condition.toString() + "AND KIND = '" + status + "' ");

		GRAT029DAO grat029Daod = new GRAT029DAO(dbManager, conn);
		grat029Daod.delete(condition.toString() + "AND KIND = '" + status + "' ");

		GRAT031DAO grat031Daod = new GRAT031DAO(dbManager, conn);
		grat031Daod.delete(condition.toString() + "AND KINDS = '" + status + "' ");

		if (logList != null && logList.size() > 0) {
			String facultyCodeKind04 = DataGetter.getFacultyCodeForKind05(dbManager, conn, aStudent);
			for (int j = 0; j < logList.size(); j++) {
				CheckingLog cLog = (CheckingLog) logList.get(j);

				// 科目有效時才計算學分
				if (IS_VALID.equals(cLog.getIsValid()) || IS_MULTI_CRSNO_NCRD.equals(cLog.getIsValid()) || IS_MULTI_CRSNO_PART.equals(cLog.getIsValid())) {
					// 20090220新增, 計算本校修得學分
					if (aStudent.getGradMajorFaculty().equals(cLog.getFacultyCode()) && !"2".equals(cLog.getGetManner())) {
						getCrd2 += Integer.parseInt(cLog.getCrd());
					} else if (facultyCodeKind04.equals(cLog.getFacultyCode()) && !"2".equals(cLog.getGetManner())) {
						getCrd1 += Integer.parseInt(cLog.getCrd());
					}

					if ("90".equals(cLog.getFacultyCode()) && NOT_ADOPT.equals(cLog.getIsAdopt())
							&& ("1".equals(cLog.getGetManner()) || "2".equals(cLog.getGetManner()))) {
						common += Integer.parseInt(cLog.getCrd());// 共同課程
						GradUtil.putCommonMap(commonKind, commonMap, cLog, COMMON_PRINT_4);
					} else if ("90".equals(cLog.getFacultyCode()) && IS_ADOPT.equals(cLog.getIsAdopt())
							&& ("1".equals(cLog.getGetManner()) || "2".equals(cLog.getGetManner()))) {
						commonAdopt += Integer.parseInt(cLog.getCrd());// 共同課程申請歸併
						GradUtil.putCommonMap(commonKind, commonMap, cLog, COMMON_PRINT_4);
					} else if (aStudent.getGradMajorFaculty().equals(cLog.getFacultyCode()) && NOT_ADOPT.equals(cLog.getIsAdopt())
							&& ("1".equals(cLog.getGetManner()) || "2".equals(cLog.getGetManner()))) {
						major += Integer.parseInt(cLog.getCrd());// 主修學系
						GradUtil.putResultMap(resultMap, cLog, cLog.getIsAdopt());
					} else if (aStudent.getGradMajorFaculty().equals(cLog.getFacultyCode()) && IS_ADOPT.equals(cLog.getIsAdopt())
							&& ("1".equals(cLog.getGetManner()) || "2".equals(cLog.getGetManner()))) {
						adopt += Integer.parseInt(cLog.getCrd());// 申請歸併
						GradUtil.putResultMap(resultMap, cLog, cLog.getIsAdopt());
					} else if (!aStudent.getGradMajorFaculty().equals(cLog.getFacultyCode())
							&& ("1".equals(cLog.getGetManner()) || "2".equals(cLog.getGetManner()))) {
						other += Integer.parseInt(cLog.getCrd());// 其他選修
						GradUtil.putResultMap(resultMap, cLog, cLog.getIsAdopt());
					} else if ("90".equals(cLog.getFacultyCode()) && TO_SUMMER.equals(cLog.getGetManner()) && NOT_ADOPT.equals(cLog.getIsAdopt())) {
						summerCommon += Integer.parseInt(cLog.getCrd());// 暑修共同課程
						GradUtil.putCommonMap(commonKind, commonMap, cLog, COMMON_PRINT_4);
					} else if ("90".equals(cLog.getFacultyCode()) && TO_SUMMER.equals(cLog.getGetManner()) && IS_ADOPT.equals(cLog.getIsAdopt())) {
						summerCommonAdopt += Integer.parseInt(cLog.getCrd());// 暑修共同課程申請歸併
						GradUtil.putCommonMap(commonKind, commonMap, cLog, COMMON_PRINT_4);
					} else if (aStudent.getGradMajorFaculty().equals(cLog.getFacultyCode()) && TO_SUMMER.equals(cLog.getGetManner())
							&& NOT_ADOPT.equals(cLog.getIsAdopt())) {
						summer += Integer.parseInt(cLog.getCrd());// 暑修主修學系
						GradUtil.putResultMap(resultMap, cLog, cLog.getIsAdopt());
					} else if (aStudent.getGradMajorFaculty().equals(cLog.getFacultyCode()) && TO_SUMMER.equals(cLog.getGetManner())
							&& IS_ADOPT.equals(cLog.getIsAdopt())) {
						summerAdopt += Integer.parseInt(cLog.getCrd());// 暑修申請歸併
						GradUtil.putResultMap(resultMap, cLog, cLog.getIsAdopt());
					} else if (!aStudent.getGradMajorFaculty().equals(cLog.getFacultyCode()) && TO_SUMMER.equals(cLog.getGetManner())) {
						summerOther += Integer.parseInt(cLog.getCrd());// 暑修其他選修
						GradUtil.putResultMap(resultMap, cLog, cLog.getIsAdopt());
					} else if ("90".equals(cLog.getFacultyCode()) && "3".equals(cLog.getGetManner()) && NOT_ADOPT.equals(cLog.getIsAdopt())) {
						popCommonMajor += Integer.parseInt(cLog.getCrd());// 推廣共同課程主修學系
						GradUtil.putCommonMap(commonKind, commonMap, cLog, COMMON_PRINT_4);
					} else if ("90".equals(cLog.getFacultyCode()) && "3".equals(cLog.getGetManner()) && IS_ADOPT.equals(cLog.getIsAdopt())) {
						popCommonAdopt += Integer.parseInt(cLog.getCrd());// 推廣共同課程申請歸併
						GradUtil.putCommonMap(commonKind, commonMap, cLog, COMMON_PRINT_4);
					} else if (aStudent.getGradMajorFaculty().equals(cLog.getFacultyCode()) && "3".equals(cLog.getGetManner())
							&& NOT_ADOPT.equals(cLog.getIsAdopt())) {
						popMajor += Integer.parseInt(cLog.getCrd());// 推廣主修學系
						GradUtil.putResultMap(resultMap, cLog, cLog.getIsAdopt());
					} else if (aStudent.getGradMajorFaculty().equals(cLog.getFacultyCode()) && "3".equals(cLog.getGetManner())
							&& IS_ADOPT.equals(cLog.getIsAdopt())) {
						popAdopt += Integer.parseInt(cLog.getCrd());// 推廣申請歸併
						GradUtil.putResultMap(resultMap, cLog, cLog.getIsAdopt());
					} else if (!aStudent.getGradMajorFaculty().equals(cLog.getFacultyCode()) && "3".equals(cLog.getGetManner())) {
						popOther += Integer.parseInt(cLog.getCrd());// 推廣其他選修
						GradUtil.putResultMap(resultMap, cLog, cLog.getIsAdopt());
					}
				}

				// 新增Log
				Hashtable hashtable = transformer.poToHashtable(cLog);
				grat030Dao = new GRAT030DAO(dbManager, conn, hashtable, session);
				grat030Dao.insert();
			}
		}

		// 補零
		GradUtil.setZeroResultMap(facultyCodeList, resultMap);
		
		int total = adopt + major + summer + summerAdopt + summerOther + summerCommon + summerCommonAdopt + other + common + commonAdopt + popAdopt + popMajor + popCommonMajor + popCommonAdopt + popOther + remainCrd;

		// 處理減修學分
		int reduce = 0;
		if (reducedCrdMap.get(aStudent.getStno()) != null) {
			reduce = Integer.parseInt((String) reducedCrdMap.get(aStudent.getStno()));
		}

		// check總學分
		if (total + reduce < Integer.parseInt(parameters.getTotal())) {
			errorMsg.append(Reasons.TOTAL_NOT_ENOUGH);
			errorMsgCode.append(Reasons.TOTAL_NOT_ENOUGH_CODE);
		}

		// check共同課程(舊制雙主修無需檢查此項目)
		// 處理通識 (舊制雙主修無需檢查此項目)
		
		// check主修學系
		if ((adopt + major + summer + summerAdopt + popAdopt + popMajor) < Integer.parseInt(parameters.getMajor())) {
			errorMsg.append(Reasons.MAJOR_05_NOT_ENOUGH);
			errorMsgCode.append(Reasons.MAJOR_05_NOT_ENOUGH_CODE);

			if ("841618917".equals(aStudent.getStno())) {
				errorMsgCode.append(Reasons.MAJOR_2_NOT_ENOUGH_CODE_N);
			}
		}

		// check採計上限
		int pAdopt = GradUtil.getAdoptCrdParameter(adoptCrdList, aStudent, aStudent.getGradMajorFaculty());// 採計他系上限
		if (pAdopt != -1) {
			if ((adopt + summerAdopt + popAdopt) > pAdopt) {
				errorMsg.append(Reasons.ADOPT_OVER);
				errorMsgCode.append(Reasons.ADOPT_OVER_CODE);
			}
		}

		// check暑修上限
		if ((summer + summerAdopt + summerOther + summerCommon + summerCommonAdopt) > Integer.parseInt(parameters.getSummer())) {
			errorMsg.append(Reasons.SUMMER_OVER);
			errorMsgCode.append(Reasons.SUMMER_OVER_CODE);
		}

		// check推廣上限
		if ((popAdopt + popMajor + popCommonMajor + popCommonAdopt + popOther) > Integer.parseInt(parameters.getPop())) {
			errorMsg.append(Reasons.POP_OVER);
			errorMsgCode.append(Reasons.POP_OVER_CODE);
		}

		// check主修學系一本校修得
		if (getCrd1 < Integer.parseInt(parameters.getDoubleOpen1())) {
			errorMsg.append(Reasons.CODE_1_NOT_ENOUGH);
			errorMsgCode.append(Reasons.CODE_1_NOT_ENOUGH_CODE);
		}

		// check主修學系二本校修得
		if (getCrd2 < Integer.parseInt(parameters.getDoubleOpen2())) {
			errorMsg.append(Reasons.CODE_2_NOT_ENOUGH);
			errorMsgCode.append(Reasons.CODE_2_NOT_ENOUGH_CODE);
		}
		
		//by poto 處理必修
		if(!checkObligatoryCrd(aStudent.getGradMajorFaculty(),CrsnoList.toString())){
			errorMsg.append(Reasons.REQUIRED_NOT_ENOUGH);
			errorMsgCode.append(Reasons.REQUIRED_NOT_ENOUGH_CODE);
		}
		
		
		//修得學分數
		if (passCrd < Integer.parseInt(this.parameters.getPassCrd())) {
			errorMsg.append(Reasons.PASS_CRD_ENOUGH);
			errorMsgCode.append(Reasons.PASS_CRD_ENOUGH_CODE);
		}

		// check是否符合畢業資格
		Hashtable ht = new Hashtable();
		if ("".equals(errorMsg.toString().trim()) && "".equals(errorMsgCode.toString().trim())) {
			errorMsgCode.append(Reasons.AGREE_AUDIT_CODE);// 同意複審
			if (FIRST_VERIFY.equals(status)) {
				ht.put("AUTO_AUDIT_STATUS", PASS);
				ht.put("AUTO_AUDIT_UNQUAL_CAUSE", "");
			} else if (LAST_VERIFY.equals(status)) {
				ht.put("GRAD_REEXAM_STATUS", PASS);
				ht.put("GRAD_REEXAM_UNQUAL_CAUSE", "");
				ht.put("GRADE_SCORE", DataGetter.getGrade(dbManager, conn, aStudent, OLD_DOUBLE));// 成績
				ht.put("GRAD_PROVE_NUMBER_1", GradUtil.getSenquence(dbManager, conn, session, aStudent.getAyear(), aStudent.getSms(), "1"));// 畢業證書號
				ht.put("GRAD_DATE", GradUtil.generateGradMonth("2", aStudent.getAyear(), aStudent.getSms()));// 畢業年月
			}
		} else {
			if (FIRST_VERIFY.equals(status)) {
				ht.put("AUTO_AUDIT_STATUS", NOT_PASS);
				ht.put("AUTO_AUDIT_UNQUAL_CAUSE", errorMsg.toString());
			} else if (LAST_VERIFY.equals(status)) {
				ht.put("GRAD_REEXAM_STATUS", NOT_PASS);
				ht.put("GRAD_REEXAM_UNQUAL_CAUSE", errorMsg.toString());
				ht.put("GRADE_SCORE", DataGetter.getGrade(dbManager, conn, aStudent, OLD_DOUBLE));// 成績
				ht.put("GRAD_PROVE_NUMBER_1", "");// 畢業證書號
				ht.put("GRAD_DATE", "");// 畢業年月
			}
		}

		// 存放共同課程
		resultMap.put("90", new Integer((common + summerCommon + popCommonMajor)));
		resultMap.put("90" + ADOPT_NAME, new Integer((commonAdopt + popCommonAdopt + summerCommonAdopt)));

		// 將grat003的狀態改為通過或不通過，並加上不通過原因
		StringBuffer sql = new StringBuffer();
		sql.append("AYEAR = '" + aStudent.getAyear() + "' ");
		sql.append("AND SMS = '" + aStudent.getSms() + "' ");
		sql.append("AND STNO = '" + aStudent.getStno() + "' ");

		GRAT003DAO grat003Dao = new GRAT003DAO(dbManager, conn, ht, session);
		grat003Dao.update(sql.toString());
		
		//by poto
		ht = new Hashtable();
		ht.put("GRADE_SCORE", DataGetter.getGrade(dbManager, conn, aStudent, OLD_DOUBLE));// 成績
		grat003Dao = new GRAT003DAO(dbManager, conn, ht, session);		
		grat003Dao.update(sql.toString());
		
		// 畢業電腦初、複審詳細資料(ONE)
		VerifyDetail vd = new VerifyDetail();
		vd.setAyear(aStudent.getAyear());
		vd.setSms(aStudent.getSms());
		vd.setStno(aStudent.getStno());
		vd.setKind(status);
		vd.setTotal(String.valueOf(total));
		vd.setPop(String.valueOf((popAdopt + popMajor + popCommonMajor + popCommonAdopt + popOther)));
		vd.setSummer(String.valueOf((summer + summerOther + summerCommon + summerAdopt + summerCommonAdopt)));
		vd.setSummerWithoutAdopt(String.valueOf(summerWithoutAdopt));
		vd.setReduce(String.valueOf(crdAdopt.getReduceCRD(dbManager, conn, aStudent.getAyear(), aStudent.getSms(), aStudent.getStno())));
		vd.setResult(errorMsgCode.toString());

		Hashtable hashtable = transformer.poToHashtable(vd);
		GRAT028DAO grat028Dao = new GRAT028DAO(dbManager, conn, hashtable, session);
		grat028Dao.insert();

		// 畢業電腦初、複審詳細資料_各學系學分與採計學分(MULTI)
		Iterator iter = resultMap.keySet().iterator();
		while (iter.hasNext()) {
			String key = (String) iter.next();

			if (key.indexOf(ADOPT_NAME) != -1) {
				continue;
			}

			VerifyFaculty vf = new VerifyFaculty();

			vf.setAyear(aStudent.getAyear());
			vf.setSms(aStudent.getSms());
			vf.setStno(aStudent.getStno());
			vf.setKind(status);
			vf.setFacultyCode(key);

			Integer fTotal = (Integer) resultMap.get(key);
			if (fTotal != null) {
				vf.setFacultyTotal(String.valueOf(fTotal.intValue()));
			} else {
				vf.setFacultyTotal("0");
			}

			Integer aTotal = (Integer) resultMap.get(key + ADOPT_NAME);
			if (aTotal != null) {
				vf.setAdoptTotal(String.valueOf(aTotal.intValue()));
			} else {
				vf.setAdoptTotal("0");
			}

			Hashtable hashtableVf = transformer.poToHashtable(vf);
			GRAT029DAO grat029Dao = new GRAT029DAO(dbManager, conn, hashtableVf, session);
			grat029Dao.insert();
		}

		// 畢業電腦初、複審詳細資料_共同科學類學分(MULTI)
		iter = commonMap.keySet().iterator();
		while (iter.hasNext()) {
			String key = (String) iter.next();

			VerifyCommon vc = new VerifyCommon();

			vc.setAyear(aStudent.getAyear());
			vc.setSms(aStudent.getSms());
			vc.setStno(aStudent.getStno());
			vc.setKinds(status);
			vc.setKind(COMMON_PRINT_4);
			vc.setGroupCode(key);

			Integer cTotal = (Integer) commonMap.get(key);
			if (cTotal != null) {
				vc.setTotalCrd(String.valueOf(cTotal.intValue()));
			} else {
				vc.setTotalCrd("0");
			}

			Hashtable hashtableVc = transformer.poToHashtable(vc);
			GRAT031DAO grat031Dao = new GRAT031DAO(dbManager, conn, hashtableVc, session);
			grat031Dao.insert();
		}
	}

	// 取得科目新的學分數
	private String getCrsnoNewCrd(CrsnoInfo ci, String facultyCode) {
		if (crdChangeList != null && crdChangeList.size() > 0) {
			for (int i = 0; i < crdChangeList.size(); i++) {
				CrdChange crdChange = (CrdChange) crdChangeList.get(i);
				if (crdChange.getFacultyCode().equals(facultyCode)) {
					if (crdChange.getCrsno().equals(ci.getCrsno())) {
						int oldCrd = Integer.parseInt(ci.getCrd());
						int newCrd = Integer.parseInt(crdChange.getAdoptCrd());
						if ((oldCrd - newCrd) > 0 && IS_ADD_CRD_TO_TOTAL.equals(crdChange.getIsAddCrdToTotal())) {
							remainCrd += (oldCrd - newCrd);
						}

						return String.valueOf(newCrd);
					}
				}
			}
		}

		return ci.getCrd();
	}

	// 取得科目新的學分數
	private String getCrsnoNewCrd(CrsnoAdopt ca) {
		if (crdChangeList != null && crdChangeList.size() > 0) {
			for (int i = 0; i < crdChangeList.size(); i++) {
				CrdChange crdChange = (CrdChange) crdChangeList.get(i);
				if (crdChange.getFacultyCode().equals(ca.getFacultyCode())) {
					if (crdChange.getCrsno().equals(ca.getCrsno())) {
						int oldCrd = Integer.parseInt(ca.getCrd());
						int newCrd = Integer.parseInt(crdChange.getAdoptCrd());
						if ((oldCrd - newCrd) > 0 && IS_ADD_CRD_TO_TOTAL.equals(crdChange.getIsAddCrdToTotal())) {
							remainCrd += (oldCrd - newCrd);
						}

						return String.valueOf(newCrd);
					}
				} else if (crdChange.getFacultyCode().equals(ca.getAdoptFaculty())) {
					if (crdChange.getCrsno().equals(ca.getCrsno())) {
						int oldCrd = Integer.parseInt(ca.getCrd());
						int newCrd = Integer.parseInt(crdChange.getAdoptCrd());
						if ((oldCrd - newCrd) > 0 && IS_ADD_CRD_TO_TOTAL.equals(crdChange.getIsAddCrdToTotal())) {
							remainCrd += (oldCrd - newCrd);
						}

						return String.valueOf(newCrd);
					}
				}
			}
		}

		return ca.getCrd();
	}


	//new
	private boolean checkObligatoryCrd(String facultyCode ,String CrsnoList) {
		boolean c = false;
		if(obligatoryAllMap.get(facultyCode)!=null){
			Map facMp= (Map)obligatoryAllMap.get(facultyCode);
			Iterator key = facMp.keySet().iterator();
			c = false;
			while (key.hasNext()) {
	            Vector vt = (Vector)facMp.get((String)key.next());
	            int check = 0;
	            c = false;
	            for (int i = 0; i < vt.size(); i++){
					Hashtable ht = (Hashtable)vt.get(i);
					String CRSNO = (String)ht.get("CRSNO");
					if(CrsnoList.indexOf(CRSNO)!=-1){
						check++;
					}else{						
						break;
					}
					if(check==vt.size()){
						c = true;						
					}
				}
	            if(c){	            	
	            	break;
	            }
			}
			if(!c){	            	
            	return false;
            }
		}else{
			return true;	
		}
		return c;
	}
	
	
	/**
	 * new通識課程檢查
	 * */
	private String checkGelCrd(ApplyStudents aStudent, Vector vtMain, GraParameters graParameters, String crsnoList) throws Exception {
		StringBuffer msg = new StringBuffer();
		// i 2_中國語文
		// j 3_外國語文
		// k 4_資訊素養
		// 1 5_人文與藝術
		// 2 6_社會與法治
		// 3 7_健康與環境
		// X 8_通識教育講座
		try {
			// 減免科目
			String gelCrsnoList = ParameterUtil.getGelReduce(dbManager, conn, aStudent.getStno());

			// 計算所有學類的學分數
			Hashtable crdHt = new Hashtable();
			for (int k = 0; k < vtMain.size(); k++) {
				String DISCIPLINE_CODE = "";
				int crdSum = 0;
				Vector vt = (Vector) vtMain.get(k);
				// 每一組 DISCIPLINE_CODE
				for (int i = 0; i < vt.size(); i++) {
					Hashtable ht = (Hashtable) vt.get(i);
					DISCIPLINE_CODE = Utility.nullToSpace(ht.get("DISCIPLINE_CODE"));
					if (crsnoList.indexOf(Utility.nullToSpace(ht.get("CRSNO"))) != -1) {
						crdSum += Integer.parseInt(Utility.nullToSpace(ht.get("CRD")));
					}
				}
				crdHt.put(DISCIPLINE_CODE, crdSum + "");
			}

			// i,j,k
			// 1.i_2
			if (Integer.parseInt(graParameters.getGeneralI()) > this.getGelCrd(crdHt, new String[] { ParameterUtil.DISCIPLINE_CODE_2 }) && gelCrsnoList.indexOf("I10001")==-1 ) {
				msg.append(ParameterUtil.DISCIPLINE_CODE_2 + ",");
			}
			// 2.j_3
			if (Integer.parseInt(graParameters.getGeneralJ()) > this.getGelCrd(crdHt, new String[] { ParameterUtil.DISCIPLINE_CODE_3 }) && gelCrsnoList.indexOf("J10001")==-1 ) {
				msg.append(ParameterUtil.DISCIPLINE_CODE_3 + ",");
			}
			// 3.i_4
			if (Integer.parseInt(graParameters.getGeneralK()) > this.getGelCrd(crdHt, new String[] { ParameterUtil.DISCIPLINE_CODE_4 }) && gelCrsnoList.indexOf("K10001")==-1 ) {
				msg.append(ParameterUtil.DISCIPLINE_CODE_4 + ",");
			}
			// 4.total
			if (Integer.parseInt(graParameters.getGeneralTotal()) > this.getGelCrd(crdHt, new String[] { ParameterUtil.DISCIPLINE_CODE_2, ParameterUtil.DISCIPLINE_CODE_3, ParameterUtil.DISCIPLINE_CODE_4 })) {
				if (!(ParameterUtil.GRAD_KIND_C.equals(aStudent.getGradKind()) && (gelCrsnoList.indexOf("N10001") != -1))) {
					msg.append(ParameterUtil.DISCIPLINE_CODE_2 + ",");
					msg.append(ParameterUtil.DISCIPLINE_CODE_3 + ",");
					msg.append(ParameterUtil.DISCIPLINE_CODE_4 + ",");
				}
			}

			// group 1,2,3,
			if (Integer.parseInt(graParameters.getGeneralGroup1()) > this.getGelCrd(crdHt, new String[] { ParameterUtil.DISCIPLINE_CODE_5 })) {
				msg.append(ParameterUtil.DISCIPLINE_CODE_5 + ",");
			}

			if (Integer.parseInt(graParameters.getGeneralGroup2()) > this.getGelCrd(crdHt, new String[] { ParameterUtil.DISCIPLINE_CODE_6 })) {
				msg.append(ParameterUtil.DISCIPLINE_CODE_6 + ",");
			}

			if (Integer.parseInt(graParameters.getGeneralGroup3()) > this.getGelCrd(crdHt, new String[] { ParameterUtil.DISCIPLINE_CODE_7 })) {
				msg.append(ParameterUtil.DISCIPLINE_CODE_7 + ",");
			}
			

			if (Integer.parseInt(graParameters.getGeneralGroupTotal()) > this.getGelCrd(crdHt, new String[] { ParameterUtil.DISCIPLINE_CODE_5, ParameterUtil.DISCIPLINE_CODE_6, ParameterUtil.DISCIPLINE_CODE_7 })) {
				if (!(ParameterUtil.GRAD_KIND_C.equals(aStudent.getGradKind()) && (gelCrsnoList.indexOf("O10001") != -1))) {
					msg.append(ParameterUtil.DISCIPLINE_CODE_5 + ",");
					msg.append(ParameterUtil.DISCIPLINE_CODE_6 + ",");
					msg.append(ParameterUtil.DISCIPLINE_CODE_7 + ",");
				}
			}

			// 講座於111上學期皆更改為核心課程
			//if (Integer.parseInt(graParameters.getCourse()) > this.getGelCrd(crdHt, new String[] { ParameterUtil.DISCIPLINE_CODE_8 })) {
			//	msg.append(ParameterUtil.DISCIPLINE_CODE_8 + ",");
			//}

			// 講座+核心
			//if (Integer.parseInt(graParameters.getGroupCourse()) > this.getGelCrd(crdHt, new String[] { ParameterUtil.DISCIPLINE_CODE_5, ParameterUtil.DISCIPLINE_CODE_6, ParameterUtil.DISCIPLINE_CODE_7, ParameterUtil.DISCIPLINE_CODE_8 })) {
			if (Integer.parseInt(graParameters.getGroupCourse()) > this.getGelCrd(crdHt, new String[] { ParameterUtil.DISCIPLINE_CODE_5, ParameterUtil.DISCIPLINE_CODE_6, ParameterUtil.DISCIPLINE_CODE_7})) {
				msg.append(ParameterUtil.DISCIPLINE_CODE_5 + ",");
				msg.append(ParameterUtil.DISCIPLINE_CODE_6 + ",");
				msg.append(ParameterUtil.DISCIPLINE_CODE_7 + ",");
				//msg.append(ParameterUtil.DISCIPLINE_CODE_8 + ",");
			}

		} catch (Exception ex) {
			throw ex;
		}
		return msg.toString();
	}
	
	/**
	 * 通識
	 * */
	private int getGelCrd(Hashtable crdHt, String[] codeAry) {
		int crd = 0;
		for (int i = 0; i < codeAry.length; i++) {
			crd += Integer.parseInt(Utility.nullToSpace(crdHt.get(codeAry[i])));
		}
		return crd;
	}
}
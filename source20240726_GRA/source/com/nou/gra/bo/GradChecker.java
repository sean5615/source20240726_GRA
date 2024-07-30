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

	public static final String WAITING = "1";// �ݼf�d
	public static final String PASS = "2";// �q�L
	public static final String NOT_PASS = "3";// ���q�L

	public static final String ASYS_OPEN = "1";// �Ťj�Ǩ�

	public static final String SINGLE = "01";// ��D��
	public static final String DOUBLE = "02";// ���D��
	public static final String OLD_DOUBLE = "05";// �¨����D��

	public static final String FIRST_VERIFY = "1";// ��f���A
	public static final String LAST_VERIFY = "2";// �Ƽf���A

	public static final String COMMON_PRINT_1 = "1";// �@�P�ҵ{��1
	public static final String COMMON_PRINT_2 = "2";// �@�P�ҵ{��2
	public static final String COMMON_PRINT_3 = "3";// �@�P�ҵ{��3
	public static final String COMMON_PRINT_4 = "4";// �@�P�ҵ{��4

	public static final String FROM_SUMMER = "3";// ���׽ҵ{
	public static final String IS_SUMMER = "Y";// �O�_�����׽ҵ{
	public static final String TO_SUMMER = "5";// ���׽ҵ{
	public static final String TO_THIS_YEAR = "4";// ��Ǧ~���ת���

	public static final String IS_VALID = "Y";// ���Ĭ��
	public static final String NOT_VALID = "N";// �L�Ĭ��
	public static final String IS_MULTI_CRSNO_PART = "B";// �L�Ĭ�� �h����������
	public static final String IS_MULTI_CRSNO_NCRD = "G";// �L�Ĭ�� �h���N�Ǥ�
	
	public static final String NOT_VALID_NAME = "�L�Ĭ��"; // �L�Ĭ��
	public static final String IS_MULTI_CRSNO_PART_NAME = "�h���N��";// �L�Ĭ�� 
	public static final String IS_MULTI_CRSNO_NCRD_NAME = "�h���N�Ǥ�";// �L�Ĭ�� 

	public static final String IS_ADOPT = "Y";// �ĭp
	public static final String NOT_ADOPT = "N";// ���ĭp

	public static final String IS_ADD_CRD_TO_TOTAL = "Y";// �N�Ѿl�Ǥ��[�J�`�Ǥ�

	public static final String ADOPT_NAME = "_adopt";// �L�Ĭ��

	public static final String CAN_GRAD = "5";// ���~���A
	public static final String CAN_NOT_GRAD = "2";// �b�y���A
	
	

	protected JspWriter out;
	protected DBManager dbManager;
	protected Hashtable requestMap;
	protected HttpSession session;
	protected Connection conn;

	protected Parameters parameters;// ���թʰѼ�
	protected Hashtable graParamHt; // ���թʲ��~�Ѽ�
	protected List deadlineList;// ��ئ��Ĵ����Ѽ�
	protected List crdChangeList;// ��ؾǤ��ܰʰѼ�
	protected List adoptCrdList;// �ĭp�L�t�Ǥ��W��
	protected List multiCrsnoList;// �h����@���N�Ǥ��Ѽ�
	protected Map commonKind;// �@�P�ҵ{��ظs�չ�Ӫ�
	protected List facultyCodeList;// �Ǩt�N�X

	protected Transformer transformer;
	protected CrdAdopt crdAdopt;

	protected int remainCrd;// �O���h����@�쪺�Ѿl�Ǥ�
	protected Map reducedCrdMap;
	protected int old;
	protected int new1;
	protected int new2;
	protected Map obligatoryMap;
	protected Map obligatoryAllMap;
	protected Map summerMap;
	protected int multiCrd;// �O���h����@�쪺�Ǥ�
	protected int nouCrd;// �Ļ{�Ǥ�
	protected Map nouMap;// �Ļ{���
	protected Map crsnoMap;// �s�¬�ع��(�s)
	protected Map CommonCrsnoMap;// ��t�@�}
	/** �q�ѽҵ{��� */
	protected Vector gelVector;

	protected String errMsg = ""; // ���~�T��
	
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

		// ���X���թʰѼ�
		parameters = ParameterUtil.getParameters(this.dbManager, this.requestMap, this.conn);
		if (parameters != null) {
			graParamHt = parameters.getGraHt();
		}
		
		deadlineList = ParameterUtil.getDeadline(this.dbManager, this.requestMap, this.conn);// ���Ĵ���
		crdChangeList = ParameterUtil.getCrdChange(this.dbManager, this.requestMap, this.conn);// �Ǥ��ܰ�
		// �ۦP�ʽ���

		adoptCrdList = ParameterUtil.getAdoptCrd(this.dbManager, this.requestMap, this.conn);// �L�t�ĭp
		multiCrsnoList = ParameterUtil.getMultiCrsno(this.dbManager, this.requestMap, this.conn);// �h����@���N�Ǥ��Ѽ�

		transformer = new Transformer();
		crdAdopt = new CrdAdopt(new Hashtable());
		commonKind = ParameterUtil.getCommonKind(this.dbManager, this.conn);
		facultyCodeList = ParameterUtil.getFacultyCodeList(this.dbManager, this.conn);

		reducedCrdMap = ParameterUtil.getReducedCrdMap(this.dbManager, this.requestMap, this.conn);// ��׾Ǥ�
		obligatoryMap = ParameterUtil.getObligatoryMap(this.dbManager, this.requestMap, this.conn);// ���׬��
		obligatoryAllMap = ParameterUtil.getObligatoryAllMap(this.dbManager, this.requestMap, this.conn);// ���׬�ظs��new
		summerMap = ParameterUtil.getSummerInfoMap(this.dbManager, this.conn);// �����}�ҫo�D���׽ҵ{
		nouMap = ParameterUtil.getNouInfoMap(this.dbManager, this.requestMap, this.conn);
		crsnoMap = ParameterUtil.getCrsnoInfoMap(this.dbManager, this.requestMap, this.conn);// �s�¬�ع��
		CommonCrsnoMap = ParameterUtil.getCommonCrsnoMap(this.dbManager, this.conn);// ��t�@�}
		gelVector = ParameterUtil.getGelVector(this.dbManager, this.conn); //�q�ѽҵ{
	}

	public void run() throws Exception {
		// do nothing
	}

	protected void doVerify(String status) throws Exception {
		// ���X��Ǧ~���Ҧ��Ǩt��f���A�B�@�P���f���A�B�Ƽf���A���ݼf�d���Ҧ��ǥ�
		List list = DataGetter.getApplyStudents(dbManager, conn, requestMap, status);
		if (list != null && list.size() > 0) {
			this.session.setAttribute("check_total_count", new Integer(list.size()));
			for (int i = 0; i < list.size(); i++) {
				ApplyStudents aStudent = (ApplyStudents) list.get(i);
				
				remainCrd = 0;// �Ѿl�Ǥ��`��
				multiCrd = 0;
				int summerWithoutAdopt = 0;// ���פ��ĭp�Ǥ��`��
				old = 0;// ���׬�ئ�F��
				new1 = 0;// ���׬�ئ�F��(�@)(�W)
				new2 = 0;// ���׬�ئ�F��(�G)(�U)
				nouCrd = 0;// �Ļ{�Ǥ�

				List logList = new ArrayList();// �O����
				List mList = new ArrayList();// �h����@���N�Ǥ����Ȧs��
				// List studyingCrsnoList =
				// ParameterUtil.getStudyingCrsno(dbManager, conn, aStudent);//
				// ��Ǧ~���ת���
				List studyingCrsnoList = new ArrayList();// ��Ǧ~���ת���
				StringBuffer errorMsg = new StringBuffer();// ���q�L��]
				StringBuffer errorMsgCode = new StringBuffer();// ���q�L��]�N�X

				// ��D��
				if (SINGLE.equals(aStudent.getAppGradType())) {
					this.processKind01(aStudent, errorMsg, errorMsgCode, logList, mList, summerWithoutAdopt, studyingCrsnoList, status);
				} else if (DOUBLE.equals(aStudent.getAppGradType())) {// ���D��
					this.processKind02(aStudent, errorMsg, errorMsgCode, logList, mList, summerWithoutAdopt, studyingCrsnoList, status);
				} else if (OLD_DOUBLE.equals(aStudent.getAppGradType())) {// �¨����D��
					this.processKind05(aStudent, errorMsg, errorMsgCode, logList, mList, summerWithoutAdopt, studyingCrsnoList, status);
				}

				System.out.println(i+1 + "/" + list.size());
				this.session.setAttribute("check_current_count", new Integer(i + 1));
			}
		}
	}

	protected void processKind01(ApplyStudents aStudent, StringBuffer errorMsg, StringBuffer errorMsgCode, List logList, List mList, int summerWithoutAdopt,
			List studyingCrsnoList, String status) throws Exception {
		// ���o�ǥͭױo���Ҧ����
		List aCrsno = DataGetter.getAllCrsno(dbManager, conn, aStudent, false);
		//by poto �p��ױo�Ǥ�
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
				
				// �P�_�O�_�����׽ҵ{
				String key = ci.getAyear() + ci.getSms() + ci.getCrsno();
				if ("1".equals(ci.getSms()) || "2".equals(ci.getSms()) || "2".equals(ci.getGetManner()) || summerMap.get(key) != null) {
					// �O������k�ݾǨt��LOG
					CheckingLog cLog = new CheckingLog();
					cLog.setAyear(aStudent.getAyear());// �ӽоǦ~
					cLog.setSms(aStudent.getSms());// �ӽоǴ�
					cLog.setStno(ci.getStno());
					cLog.setKind(status);
					cLog.setFacultyCode(ci.getFacultyCode());
					cLog.setCrsno(ci.getCrsno());
					cLog.setCrd(this.getCrsnoNewCrd(ci, cLog.getFacultyCode()));
					cLog.setGetAyear(ci.getAyear());// ��ب��o�Ǧ~
					cLog.setGetSms(ci.getSms());// ��ب��o�Ǵ�

					// �P�_�O�_����Ǧ~���ת���(�o�䤣�|�����ת��ҵ{)
					if (status.equals(FIRST_VERIFY)) {
						cLog.setGetManner(ci.getGetManner());
					} else if (status.equals(LAST_VERIFY)) {
						cLog.setGetManner(ci.getGetManner());
					}

					// �P�_��جO�_����
					if (GradUtil.isCrsnoValid(deadlineList, cLog)) {
						cLog.setIsValid(IS_VALID);
					} else {
						cLog.setIsValid(NOT_VALID);
					}

					cLog.setIsAdopt(NOT_ADOPT);

					// �ۦP�ʽ���
					if (IS_VALID.equals(cLog.getIsValid())) {
						String tempKey = aStudent.getStno() + cLog.getGetAyear() + cLog.getGetSms() + cLog.getCrsno() + cLog.getGetManner();
						if (this.crsnoMap.get(tempKey) != null) {
							cLog.setIsValid(NOT_VALID);
						}
					}

					// �P�_��جO�_���h����������
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

		// ���o�ǥʹ��׻P�ĭp���Ҧ����
		List adoptList = DataGetter.getAdoptCrsno(dbManager, conn, aStudent);
		if (adoptList != null && adoptList.size() > 0) {
			for (int j = 0; j < adoptList.size(); j++) {
				CrsnoAdopt ca = (CrsnoAdopt) adoptList.get(j);
                //by poto 20090623 ��ﳡ�n�ư���K��
				// ��f�Ʊ���Ǧ~�������
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
				// �O������k�ݾǨt��LOG
				CheckingLog cLog = new CheckingLog();
				cLog.setAyear(aStudent.getAyear());// �ӽоǦ~
				cLog.setSms(aStudent.getSms());// �ӽоǴ�
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
				cLog.setGetAyear(ca.getGetAyear());// ��ب��o�Ǧ~
				cLog.setGetSms(ca.getGetSms());// ��ب��o�Ǵ�

				// �P�_�O�_����Ǧ~���ת���
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

				// �P�_��جO�_����
				if (GradUtil.isCrsnoValid(deadlineList, cLog)) {
					cLog.setIsValid(IS_VALID);
				} else {
					cLog.setIsValid(NOT_VALID);
				}

				// �P�_���׽ҵ{�O�_���D�׾Ǩt
				if (isSummerMajor) {
					cLog.setIsAdopt(NOT_ADOPT);
				} else {
					// 20090106�s�W, ��t�@�}��cout103�٬O�|�]�w���@�D�h�ħΦ�, ���ѭ��W�٬O�|�O����t�Ҭ��D�}
					// ��Gra002m�ӽЮɭY�M�w�N�Ӭ�ĭp���L���, �o��|�P�_��t�@�}, �ñN��אּ�D�׾Ǩt
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

				// �ۦP�ʽ���
				if (IS_VALID.equals(cLog.getIsValid())) {
					String tempKey = aStudent.getStno() + cLog.getGetAyear() + cLog.getGetSms() + cLog.getCrsno() + cLog.getGetManner();
					if (this.crsnoMap.get(tempKey) != null) {
						cLog.setIsValid(NOT_VALID);
					}
				}

				// �P�_��جO�_���h����������
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
		
		
		// �N�h���������ة�N�Ǥ�����إ[��logList
		remainCrd += GradUtil.processMultiCrsno(mList, logList);
		
		// �}�l�p��Ǥ�
		int adopt = 0;// �ӽ��k��
		int major = 0;// �D�׾Ǩt
		int majorPass = 0;// �D�׾Ǩt�ױo�Ǥ���
		int summer = 0;// ���ץD�׾Ǩt
		int summerAdopt = 0;// ���ץӽ��k��
		int summerOther = 0;// ���ר�L���
		int summerCommon = 0;// ���צ@�P�ҵ{
		int summerCommonAdopt = 0;// ���צ@�P�ҵ{�ӽ��k��
		int popAdopt = 0;// ���s�ӽ��k��
		int popMajor = 0;// ���s�D�׾Ǩt
		int popCommonMajor = 0;// ���s�@�P�ҵ{�D�׾Ǩt
		int popCommonAdopt = 0;// ���s�@�P�ҵ{�ӽ��k��
		int popOther = 0;// ���s��L���
		int other = 0;// ��L���
		int common = 0;// �@�P�ҵ{
		int commonAdopt = 0;// �@�P�ҵ{�ӽ��k��
		boolean graPassCcs109Yn =  false; //�P�_�O�_��109��CCS
		Map resultMap = new HashMap();
		Map commonMap = new HashMap();

		// �R��Log
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
				// ��ئ��Įɤ~�p��Ǥ�
				if (IS_VALID.equals(cLog.getIsValid()) || IS_MULTI_CRSNO_NCRD.equals(cLog.getIsValid()) || IS_MULTI_CRSNO_PART.equals(cLog.getIsValid())) {
					//�@�P���
				    if ("90".equals(cLog.getFacultyCode()) && NOT_ADOPT.equals(cLog.getIsAdopt())
							&& ("1".equals(cLog.getGetManner()) || "2".equals(cLog.getGetManner()))) {
						common += Integer.parseInt(cLog.getCrd());// �@�P�ҵ{
						GradUtil.putCommonMap(commonKind, commonMap, cLog, COMMON_PRINT_4);
					} else if ("90".equals(cLog.getFacultyCode()) && IS_ADOPT.equals(cLog.getIsAdopt())
							&& ("1".equals(cLog.getGetManner()) || "2".equals(cLog.getGetManner()))) {
						commonAdopt += Integer.parseInt(cLog.getCrd());// �@�P�ҵ{�ӽ��k��
						GradUtil.putCommonMap(commonKind, commonMap, cLog, COMMON_PRINT_4);
					} else if ("90".equals(cLog.getFacultyCode()) && TO_SUMMER.equals(cLog.getGetManner()) && NOT_ADOPT.equals(cLog.getIsAdopt())) {
						summerCommon += Integer.parseInt(cLog.getCrd());// ���צ@�P�ҵ{
						GradUtil.putCommonMap(commonKind, commonMap, cLog, COMMON_PRINT_4);
					} else if ("90".equals(cLog.getFacultyCode()) && TO_SUMMER.equals(cLog.getGetManner()) && IS_ADOPT.equals(cLog.getIsAdopt())) {
						summerCommonAdopt += Integer.parseInt(cLog.getCrd());// ���צ@�P�ҵ{�ӽ��k��
						GradUtil.putCommonMap(commonKind, commonMap, cLog, COMMON_PRINT_4);	
					} else if ("90".equals(cLog.getFacultyCode()) && "3".equals(cLog.getGetManner()) && NOT_ADOPT.equals(cLog.getIsAdopt())) {
						popCommonMajor += Integer.parseInt(cLog.getCrd());// ���s�@�P�ҵ{�D�׾Ǩt
						GradUtil.putCommonMap(commonKind, commonMap, cLog, COMMON_PRINT_4);
					} else if ("90".equals(cLog.getFacultyCode()) && "3".equals(cLog.getGetManner()) && IS_ADOPT.equals(cLog.getIsAdopt())) {
						popCommonAdopt += Integer.parseInt(cLog.getCrd());// ���s�@�P�ҵ{�ӽ��k��
						GradUtil.putCommonMap(commonKind, commonMap, cLog, COMMON_PRINT_4);	
					}else 
					//�@����	
					if (aStudent.getGradMajorFaculty().equals(cLog.getFacultyCode()) && NOT_ADOPT.equals(cLog.getIsAdopt())
							&& ("1".equals(cLog.getGetManner()) || "2".equals(cLog.getGetManner()))) {
						major += Integer.parseInt(cLog.getCrd());// �D�׾Ǩt
						majorPass += ("1".equals(cLog.getGetManner()))?Integer.parseInt(cLog.getCrd()):0;// �D�׹�o�Ǩt
						GradUtil.putResultMap(resultMap, cLog, cLog.getIsAdopt());
					} else if (aStudent.getGradMajorFaculty().equals(cLog.getFacultyCode()) && IS_ADOPT.equals(cLog.getIsAdopt())
							&& ("1".equals(cLog.getGetManner()) || "2".equals(cLog.getGetManner()))) {
						adopt += Integer.parseInt(cLog.getCrd());// �ӽ��k��
						majorPass += Integer.parseInt(cLog.getCrd());// �D�׹�o�D�׾Ǩt     /* 20240718�D�׹�o�Ǥ��t�ĭp�L�t�Ǥ��� */  
						GradUtil.putResultMap(resultMap, cLog, cLog.getIsAdopt());
					} else if (!aStudent.getGradMajorFaculty().equals(cLog.getFacultyCode())
							&& ("1".equals(cLog.getGetManner()) || "2".equals(cLog.getGetManner()))) {
						other += Integer.parseInt(cLog.getCrd());// ��L���
						GradUtil.putResultMap(resultMap, cLog, cLog.getIsAdopt());					
					} else if (aStudent.getGradMajorFaculty().equals(cLog.getFacultyCode()) && TO_SUMMER.equals(cLog.getGetManner())
							&& NOT_ADOPT.equals(cLog.getIsAdopt())) {
						summer += Integer.parseInt(cLog.getCrd());// ���ץD�׾Ǩt
						majorPass += Integer.parseInt(cLog.getCrd());// �D�׹�o�Ǩt
						GradUtil.putResultMap(resultMap, cLog, cLog.getIsAdopt());
					} else if (aStudent.getGradMajorFaculty().equals(cLog.getFacultyCode()) && TO_SUMMER.equals(cLog.getGetManner())
							&& IS_ADOPT.equals(cLog.getIsAdopt())) {
						summerAdopt += Integer.parseInt(cLog.getCrd());// ���ץӽ��k��    
						GradUtil.putResultMap(resultMap, cLog, cLog.getIsAdopt());
					} else if (!aStudent.getGradMajorFaculty().equals(cLog.getFacultyCode()) && TO_SUMMER.equals(cLog.getGetManner())) {
						summerOther += Integer.parseInt(cLog.getCrd());// ���ר�L���
						GradUtil.putResultMap(resultMap, cLog, cLog.getIsAdopt());					
					} else if (aStudent.getGradMajorFaculty().equals(cLog.getFacultyCode()) && "3".equals(cLog.getGetManner())
							&& NOT_ADOPT.equals(cLog.getIsAdopt())) {
						popMajor += Integer.parseInt(cLog.getCrd());// ���s�D�׾Ǩt
						GradUtil.putResultMap(resultMap, cLog, cLog.getIsAdopt());
					} else if (aStudent.getGradMajorFaculty().equals(cLog.getFacultyCode()) && "3".equals(cLog.getGetManner())
							&& IS_ADOPT.equals(cLog.getIsAdopt())) {
						popAdopt += Integer.parseInt(cLog.getCrd());// ���s�ӽ��k��
						GradUtil.putResultMap(resultMap, cLog, cLog.getIsAdopt());
					} else if (!aStudent.getGradMajorFaculty().equals(cLog.getFacultyCode()) && "3".equals(cLog.getGetManner())) {
						popOther += Integer.parseInt(cLog.getCrd());// ���s��L���
						GradUtil.putResultMap(resultMap, cLog, cLog.getIsAdopt());
					}
				}
				// �s�WLog
				Hashtable hashtable = transformer.poToHashtable(cLog);
				grat030Dao = new GRAT030DAO(dbManager, conn, hashtable, session);
				grat030Dao.insert();
				//109ccs check
				if( !graPassCcs109Yn && "2".equals(cLog.getGetManner()) && cLog.getGetAyear().compareTo(ParameterUtil.GRA_CCS_109) >= 0){
					graPassCcs109Yn = true;
				}
			}
		}
		
		//���~�Ѽ�
		GraParameters graParameters = (GraParameters) this.graParamHt.get(GradUtil.getGradKey(aStudent.getGradKind(), aStudent.getEdubkgrdAbility()));
		
		// �ɹs
		GradUtil.setZeroResultMap(facultyCodeList, resultMap);

		// ���׶W�L30�Ǥ��h�n�H30�Ǥ��p��(���t���פ��ĭp)
//		int summerAll = summer + summerAdopt + summerOther + summerCommon + summerCommonAdopt;// + summerWithoutAdopt;
//		if (summerAll > Integer.parseInt(parameters.getSummer())) {
//			summerAll = Integer.parseInt(parameters.getSummer());
//		}

		int total = adopt + major + summer + summerAdopt + summerOther + summerCommon + summerCommonAdopt + other + common + commonAdopt + popAdopt + popMajor + popCommonMajor + popCommonAdopt + popOther + remainCrd;
		//int totalForMulti = adopt + major + summerAll + other + common + commonAdopt + popAdopt + popMajor + popCommonMajor + popCommonAdopt + popOther + multiCrd;
		
		// �B�z��׾Ǥ�
		int reduce = 0;
		if (reducedCrdMap.get(aStudent.getStno()) != null) {
			reduce = Integer.parseInt((String) reducedCrdMap.get(aStudent.getStno()));
		}

		// check�`�Ǥ�
		if (total + reduce < Integer.parseInt(graParameters.getGraTotal())) {		
			errorMsg.append(Reasons.TOTAL_NOT_ENOUGH);
			errorMsgCode.append(Reasons.TOTAL_NOT_ENOUGH_CODE);
		}

		
		// check�D�׾Ǩt
		if ((adopt + major + summer + summerAdopt + popAdopt + popMajor) < Integer.parseInt(graParameters.getMajorTotal())) {
			errorMsg.append(Reasons.MAJOR_1_NOT_ENOUGH);
			errorMsgCode.append(Reasons.MAJOR_1_NOT_ENOUGH_CODE);

			if ("831181414".equals(aStudent.getStno()) || "851440283".equals(aStudent.getStno()) || "871292535".equals(aStudent.getStno())) {
				errorMsgCode.append(Reasons.MAJOR_1_NOT_ENOUGH_CODE_M);
			}
		}

		// check�ĭp�W��
		int pAdopt = GradUtil.getAdoptCrdParameter(adoptCrdList, aStudent, aStudent.getGradMajorFaculty());// �ĭp�L�t�W��
		if (pAdopt != -1) {
			// if ((adopt + commonAdopt + summerAdopt + summerCommonAdopt +
			// popAdopt + popCommonAdopt) > pAdopt) {
			if ((adopt + summerAdopt + popAdopt) > pAdopt) {
				errorMsg.append(Reasons.ADOPT_OVER);
				errorMsgCode.append(Reasons.ADOPT_OVER_CODE);
			}
		}

		// check���פW��
		if ((summer + summerAdopt + summerOther + summerCommon + summerCommonAdopt) > Integer.parseInt(parameters.getSummer())) {
			errorMsg.append(Reasons.SUMMER_OVER);
			errorMsgCode.append(Reasons.SUMMER_OVER_CODE);
		}

		// check���s�W��
		if ((popAdopt + popMajor + popCommonMajor + popCommonAdopt + popOther) > Integer.parseInt(parameters.getPop())) {
			errorMsg.append(Reasons.POP_OVER);
			errorMsgCode.append(Reasons.POP_OVER_CODE);
		}

		
		//check���׬�ظs��
		if(!checkObligatoryCrd(aStudent.getGradMajorFaculty(),CrsnoList.toString())){
			errorMsg.append(Reasons.REQUIRED_NOT_ENOUGH);
			errorMsgCode.append(Reasons.REQUIRED_NOT_ENOUGH_CODE);
		}
		
		//check�@�P�ҵ{		
		if ((common + commonAdopt + summerCommon + summerCommonAdopt + popCommonMajor + popCommonAdopt) < Integer.parseInt(graParameters.getCommon())) {
			errorMsg.append(Reasons.COMMON_NOT_ENOUGH);
			errorMsgCode.append(Reasons.COMMON_NOT_ENOUGH_CODE);
		}
		
		// check �q��
		String gelMsg = checkGelCrd(aStudent, this.gelVector, graParameters, CrsnoList.toString());
		if (!"".equals(gelMsg)) {
			dbManager.logger.append("no pass DISCIPLINE_CODE =" + gelMsg);
			errorMsg.append(Reasons.GENERAL_NOT_ENOUGH);
			errorMsgCode.append(Reasons.GENERAL_NOT_ENOUGH_CODE);
		}
		
		// ��o�Ǥ�
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
		
		// ��o�Ǥ� + 109 css
		if (graPassCcs109Yn && (passCrd < Integer.parseInt(graParameters.getGraPassCcs109()))) {
			
			errorMsg.append(Reasons.PASS_CRD_ENOUGH);
			errorMsgCode.append(Reasons.PASS_CRD_ENOUGH_CODE);
		}

		// �D�׹�o�Ǥ�
		if (majorPass < Integer.parseInt(graParameters.getMajorPass())) {
			errorMsg.append(Reasons.MAJORPASS_CRD_ENOUGH);
			errorMsgCode.append(Reasons.PASS_CRD_ENOUGH_CODE);
		}
		
		// check�O�_�ŦX���~���
		Hashtable ht = new Hashtable();
		if ("".equals(errorMsg.toString().trim()) && "".equals(errorMsgCode.toString().trim())) {
			errorMsgCode.append(Reasons.AGREE_AUDIT_CODE);// �P�N�Ƽf
			if (FIRST_VERIFY.equals(status)) {
				ht.put("AUTO_AUDIT_STATUS", PASS);
				ht.put("AUTO_AUDIT_UNQUAL_CAUSE", "");
			} else if (LAST_VERIFY.equals(status)) {
				ht.put("GRAD_REEXAM_STATUS", PASS);
				ht.put("GRAD_REEXAM_UNQUAL_CAUSE", "");
				ht.put("GRADE_SCORE", DataGetter.getGrade(dbManager, conn, aStudent, SINGLE));// ���Z
				ht.put("GRAD_PROVE_NUMBER_1", GradUtil.getSenquence(dbManager, conn, session, aStudent.getAyear(), aStudent.getSms(), "1"));// ���~�ҮѸ�
				ht.put("GRAD_DATE", GradUtil.generateGradMonth("2", aStudent.getAyear(), aStudent.getSms()));// ���~�~��				
			}
		} else {
			if (FIRST_VERIFY.equals(status)) {
				ht.put("AUTO_AUDIT_STATUS", NOT_PASS);
				ht.put("AUTO_AUDIT_UNQUAL_CAUSE", errorMsg.toString());
			} else if (LAST_VERIFY.equals(status)) {
				ht.put("GRAD_REEXAM_STATUS", NOT_PASS);
				ht.put("GRAD_REEXAM_UNQUAL_CAUSE", errorMsg.toString());
				ht.put("GRADE_SCORE", DataGetter.getGrade(dbManager, conn, aStudent, SINGLE));// ���Z
				ht.put("GRAD_PROVE_NUMBER_1", "");// ���~�ҮѸ�
				ht.put("GRAD_DATE", "");// ���~�~��
			}
		}

		// �s��@�P�ҵ{
		resultMap.put("90", new Integer((common + summerCommon + popCommonMajor)));
		resultMap.put("90" + ADOPT_NAME, new Integer((commonAdopt + popCommonAdopt + summerCommonAdopt)));

		// �Ngrat003�����A�אּ�q�L�Τ��q�L�A�å[�W���q�L��]
		StringBuffer sql = new StringBuffer();
		sql.append("AYEAR = '" + aStudent.getAyear() + "' ");
		sql.append("AND SMS = '" + aStudent.getSms() + "' ");
		sql.append("AND STNO = '" + aStudent.getStno() + "' ");

		GRAT003DAO grat003Dao = new GRAT003DAO(dbManager, conn, ht, session);
		grat003Dao.update(sql.toString());
		//by poto
		ht = new Hashtable();
		ht.put("GRADE_SCORE", DataGetter.getGrade(dbManager, conn, aStudent, SINGLE));// ���Z
		grat003Dao = new GRAT003DAO(dbManager, conn, ht, session);		
		grat003Dao.update(sql.toString());
		

		// ���~�q����B�Ƽf�ԲӸ��(ONE)
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

		// ���~�q����B�Ƽf�ԲӸ��_�U�Ǩt�Ǥ��P�ĭp�Ǥ�(MULTI)
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

		// ���~�q����B�Ƽf�ԲӸ��_�@�P������Ǥ�(MULTI)
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
		// ���o�ǥͭױo���Ҧ����
		List aCrsno = DataGetter.getAllCrsno(dbManager, conn, aStudent, false);
		int passCrd =0;		
		StringBuffer CrsnoList = new StringBuffer() ;
		if (aCrsno != null && aCrsno.size() > 0) {
			for (int j = 0; j < aCrsno.size(); j++) {
				CrsnoInfo ci = (CrsnoInfo) aCrsno.get(j);
				
				// ��f�Ʊ���Ǧ~�������
				//by poto ��f�ݭn��Ǵ�����K  �ҥH���n�ư�   ���Z�����n�i�ӴN�n(�ثe���쪺�O�������)
				//if (this.isThisYear(status, aStudent, ci.getAyear(), ci.getSms())) {
				//	continue;
				//}
				if ("1".equals(ci.getGetManner())) {
					passCrd += Integer.parseInt(ci.getCrd());
				}
				if ("3".equals(ci.getGetManner())) {
					ci.setGetManner("2");
				}

				//by poto �B�z����
				if(CrsnoList.length()!=0){					
					CrsnoList.append(","+ci.getCrsno());
				}else{					
					CrsnoList.append(ci.getCrsno());
				}

				// �P�_�O�_�����׽ҵ{
				String key = ci.getAyear() + ci.getSms() + ci.getCrsno();
				// if (!IS_SUMMER.equals(DataGetter.isSummer(dbManager, conn,
				// aStudent.getStno(), ci.getCrsno()))) {
				if ("1".equals(ci.getSms()) || "2".equals(ci.getSms()) || "2".equals(ci.getGetManner()) || summerMap.get(key) != null) {
					// �O������k�ݾǨt��LOG
					CheckingLog cLog = new CheckingLog();
					cLog.setAyear(aStudent.getAyear());// �ӽоǦ~
					cLog.setSms(aStudent.getSms());// �ӽоǴ�
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
					cLog.setGetAyear(ci.getAyear());// ��ب��o�Ǧ~
					cLog.setGetSms(ci.getSms());// ��ب��o�Ǵ�

					// �p��Ļ{�Ǥ�
					String ccst003Key = aStudent.getAyear() + aStudent.getSms() + aStudent.getStno() + ci.getCrsno();
					if (this.nouMap.get(ccst003Key) != null && "2".equals(ci.getGetManner())) {
						nouCrd += Integer.parseInt(ci.getCrd());
					}

					// �P�_�O�_����Ǧ~���ת���(�o�䤣�|�����ת��ҵ{)
					if (status.equals(FIRST_VERIFY)) {
						// ��Ǧ~������ؤw�bjoin�ɳQ�ư����F, �]�Ǥ��Ȧ�|�������, log�]���ݭn��
						// if (!GradUtil.isStudyingCrsno(studyingCrsnoList,
						// aStudent, ci)) {
						cLog.setGetManner(ci.getGetManner());
						// } else {
						// cLog.setGetManner(TO_THIS_YEAR);
						// }
					} else if (status.equals(LAST_VERIFY)) {
						cLog.setGetManner(ci.getGetManner());
					}

					// �P�_��جO�_����
					if (GradUtil.isCrsnoValid(deadlineList, cLog)) {
						cLog.setIsValid(IS_VALID);
					} else {
						cLog.setIsValid(NOT_VALID);
					}

					cLog.setIsAdopt(NOT_ADOPT);

					// �ۦP�ʽ���
					if (IS_VALID.equals(cLog.getIsValid())) {
						String tempKey = aStudent.getStno() + cLog.getGetAyear() + cLog.getGetSms() + cLog.getCrsno() + cLog.getGetManner();
						if (this.crsnoMap.get(tempKey) != null) {
							cLog.setIsValid(NOT_VALID);
						}
					}

					// �P�_��جO�_���h����������
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

		// ���o�ǥʹ��׻P�ĭp���Ҧ����
		List adoptList = DataGetter.getAdoptCrsno(dbManager, conn, aStudent);
		if (adoptList != null && adoptList.size() > 0) {
			for (int j = 0; j < adoptList.size(); j++) {
				CrsnoAdopt ca = (CrsnoAdopt) adoptList.get(j);

				// ��f�Ʊ���Ǧ~�������
				//by poto ��f�ݭn��Ǵ�����K  �ҥH���n�ư�   ���Z�����n�i�ӴN�n(�ثe���쪺�O�������)
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

				// �O������k�ݾǨt��LOG
				CheckingLog cLog = new CheckingLog();
				cLog.setAyear(aStudent.getAyear());// �ӽоǦ~
				cLog.setSms(aStudent.getSms());// �ӽоǴ�
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
				cLog.setGetAyear(ca.getGetAyear());// ��ب��o�Ǧ~
				cLog.setGetSms(ca.getGetSms());// ��ب��o�Ǵ�

				// �p��Ļ{�Ǥ�
				String ccst003Key = aStudent.getAyear() + aStudent.getSms() + aStudent.getStno() + ca.getCrsno();
				if (this.nouMap.get(ccst003Key) != null && "2".equals(ca.getGetManner())) {
					nouCrd += Integer.parseInt(ca.getCrd());
				}

				// �P�_�O�_����Ǧ~���ת���
				if (status.equals(FIRST_VERIFY)) {
					// ��Ǧ~������ؤw�bjoin�ɳQ�ư����F, �]�Ǥ��Ȧ�|�������, log�]���ݭn��
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

				// �P�_��جO�_����
				if (GradUtil.isCrsnoValid(deadlineList, cLog)) {
					cLog.setIsValid(IS_VALID);
				} else {
					cLog.setIsValid(NOT_VALID);
				}

				// �P�_���׽ҵ{�O�_���D�׾Ǩt
				if (isSummerMajor) {
					cLog.setIsAdopt(NOT_ADOPT);
				} else {
					// 20090106�s�W, ��t�@�}��cout103�٬O�|�]�w���@�D�h�ħΦ�, ���ѭ��W�٬O�|�O����t�Ҭ��D�}
					// ��Gra002m�ӽЮɭY�M�w�N�Ӭ�ĭp���L���, �o��|�P�_��t�@�}, �ñN��אּ�D�׾Ǩt
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

				// �ۦP�ʽ���
				if (IS_VALID.equals(cLog.getIsValid())) {
					String tempKey = aStudent.getStno() + cLog.getGetAyear() + cLog.getGetSms() + cLog.getCrsno() + cLog.getGetManner();
					if (this.crsnoMap.get(tempKey) != null) {
						cLog.setIsValid(NOT_VALID);
					}
				}

				// �P�_��جO�_���h����������
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

		// �N�h���������ة�N�Ǥ�����إ[��logList
		remainCrd += GradUtil.processMultiCrsno(mList, logList);

		// �}�l�p��Ǥ�
		int adopt1 = 0;// �ӽ��k��
		int adopt2 = 0;// �ӽ��k��
		int major1 = 0;// �D�׾Ǩt
		int major2 = 0;// �D�׾Ǩt
		int summer1 = 0;// ���ץD�׾Ǩt
		int summer2 = 0;// ���ץD�׾Ǩt
		int summerAdopt1 = 0;// ���ץӽ��k��
		int summerAdopt2 = 0;// ���ץӽ��k��
		int summerOther = 0;// ���ר�L���
		int summerCommon = 0;// ���צ@�P�ҵ{
		int summerCommonAdopt = 0;// ���צ@�P�ҵ{
		int popAdopt1 = 0;// ���s�ӽ��k��
		int popAdopt2 = 0;// ���s�ӽ��k��
		int popMajor1 = 0;// ���s�D�׾Ǩt
		int popMajor2 = 0;// ���s�D�׾Ǩt
		int popCommonMajor = 0;// ���s�@�P�ҵ{�D�׾Ǩt
		int popCommonAdopt = 0;// ���s�@�P�ҵ{�ӽ��k��
		int popOther = 0;// ���s��L���
		int other = 0;// ��L���
		int common = 0;// �@�P�ҵ{
		int commonAdopt = 0;// �@�P�ҵ{�ӽ��k��
		int getCrd1 = 0;// �ױo���Ǥ�
		int getCrd2 = 0;// �ױo���Ǥ�
		boolean graPassCcs109Yn =  false; //�P�_�O�_��109��CCS
		Map resultMap = new HashMap();
		Map commonMap = new HashMap();

		// �R��Log
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

				// ��ئ��Įɤ~�p��Ǥ�
				if (IS_VALID.equals(cLog.getIsValid()) || IS_MULTI_CRSNO_NCRD.equals(cLog.getIsValid()) || IS_MULTI_CRSNO_PART.equals(cLog.getIsValid())) {
					// ��f�ɷ�Ǧ~���ת��Ҥ@�߱ư�
					// ��Ǧ~������ؤw�bjoin�ɳQ�ư����F, �]�Ǥ��Ȧ�|�������, log�]���ݭn��
					// if (FIRST_VERIFY.equals(status)) {
					// if (TO_THIS_YEAR.equals(cLog.getGetManner())) {
					// continue;
					// }
					// }

					if ("90".equals(cLog.getFacultyCode()) && NOT_ADOPT.equals(cLog.getIsAdopt())
							&& ("1".equals(cLog.getGetManner()) || "2".equals(cLog.getGetManner()))) {
						common += Integer.parseInt(cLog.getCrd());// �@�P�ҵ{
						GradUtil.putCommonMap(commonKind, commonMap, cLog, COMMON_PRINT_4);
					} else if ("90".equals(cLog.getFacultyCode()) && IS_ADOPT.equals(cLog.getIsAdopt())
							&& ("1".equals(cLog.getGetManner()) || "2".equals(cLog.getGetManner()))) {
						commonAdopt += Integer.parseInt(cLog.getCrd());// �@�P�ҵ{�ӽ��k��
						GradUtil.putCommonMap(commonKind, commonMap, cLog, COMMON_PRINT_4);
					} else if (aStudent.getDbmajorGradFacultyCode1().equals(cLog.getFacultyCode()) && NOT_ADOPT.equals(cLog.getIsAdopt())
							&& ("1".equals(cLog.getGetManner()) || "2".equals(cLog.getGetManner()))) {
						major1 += Integer.parseInt(cLog.getCrd());// �D�׾Ǩt
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
						major2 += Integer.parseInt(cLog.getCrd());// �D�׾Ǩt
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
						adopt1 += Integer.parseInt(cLog.getCrd());// �ӽ��k��
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
						adopt2 += Integer.parseInt(cLog.getCrd());// �ӽ��k��
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
						other += Integer.parseInt(cLog.getCrd());// ��L���
						GradUtil.putResultMap(resultMap, cLog, cLog.getIsAdopt());
					} else if ("90".equals(cLog.getFacultyCode()) && TO_SUMMER.equals(cLog.getGetManner()) && NOT_ADOPT.equals(cLog.getIsAdopt())) {
						summerCommon += Integer.parseInt(cLog.getCrd());// ���צ@�P�ҵ{
						GradUtil.putCommonMap(commonKind, commonMap, cLog, COMMON_PRINT_4);
					} else if ("90".equals(cLog.getFacultyCode()) && TO_SUMMER.equals(cLog.getGetManner()) && IS_ADOPT.equals(cLog.getIsAdopt())) {
						summerCommonAdopt += Integer.parseInt(cLog.getCrd());// ���צ@�P�ҵ{
						GradUtil.putCommonMap(commonKind, commonMap, cLog, COMMON_PRINT_4);
					} else if (aStudent.getDbmajorGradFacultyCode1().equals(cLog.getFacultyCode()) && TO_SUMMER.equals(cLog.getGetManner())
							&& NOT_ADOPT.equals(cLog.getIsAdopt())) {
						summer1 += Integer.parseInt(cLog.getCrd());// ���ץD�׾Ǩt
						getCrd1 += Integer.parseInt(cLog.getCrd());
						GradUtil.putResultMap(resultMap, cLog, cLog.getIsAdopt());
					} else if (aStudent.getDbmajorGradFacultyCode2().equals(cLog.getFacultyCode()) && TO_SUMMER.equals(cLog.getGetManner())
							&& NOT_ADOPT.equals(cLog.getIsAdopt())) {
						summer2 += Integer.parseInt(cLog.getCrd());// ���ץD�׾Ǩt
						getCrd2 += Integer.parseInt(cLog.getCrd());
						GradUtil.putResultMap(resultMap, cLog, cLog.getIsAdopt());
					} else if (aStudent.getDbmajorGradFacultyCode1().equals(cLog.getFacultyCode()) && TO_SUMMER.equals(cLog.getGetManner())
							&& IS_ADOPT.equals(cLog.getIsAdopt())) {
						summerAdopt1 += Integer.parseInt(cLog.getCrd());// ���ץӽ��k��
						getCrd1 += Integer.parseInt(cLog.getCrd());
						GradUtil.putResultMap(resultMap, cLog, cLog.getIsAdopt());
					} else if (aStudent.getDbmajorGradFacultyCode2().equals(cLog.getFacultyCode()) && TO_SUMMER.equals(cLog.getGetManner())
							&& IS_ADOPT.equals(cLog.getIsAdopt())) {
						summerAdopt2 += Integer.parseInt(cLog.getCrd());// ���ץӽ��k��
						getCrd2 += Integer.parseInt(cLog.getCrd());
						GradUtil.putResultMap(resultMap, cLog, cLog.getIsAdopt());
					} else if (!aStudent.getDbmajorGradFacultyCode1().equals(cLog.getFacultyCode())
							&& !aStudent.getDbmajorGradFacultyCode2().equals(cLog.getFacultyCode()) && TO_SUMMER.equals(cLog.getGetManner())) {
						summerOther += Integer.parseInt(cLog.getCrd());// ���ר�L���
						GradUtil.putResultMap(resultMap, cLog, cLog.getIsAdopt());
					} else if ("90".equals(cLog.getFacultyCode()) && "3".equals(cLog.getGetManner()) && NOT_ADOPT.equals(cLog.getIsAdopt())) {
						popCommonMajor += Integer.parseInt(cLog.getCrd());// ���s�@�P�ҵ{�D�׾Ǩt
						GradUtil.putCommonMap(commonKind, commonMap, cLog, COMMON_PRINT_4);
					} else if ("90".equals(cLog.getFacultyCode()) && "3".equals(cLog.getGetManner()) && IS_ADOPT.equals(cLog.getIsAdopt())) {
						popCommonAdopt += Integer.parseInt(cLog.getCrd());// ���s�@�P�ҵ{�ӽ��k��
						GradUtil.putCommonMap(commonKind, commonMap, cLog, COMMON_PRINT_4);
					} else if (aStudent.getDbmajorGradFacultyCode1().equals(cLog.getFacultyCode()) && "3".equals(cLog.getGetManner())
							&& NOT_ADOPT.equals(cLog.getIsAdopt())) {
						popMajor1 += Integer.parseInt(cLog.getCrd());// ���s�D�׾Ǩt
						GradUtil.putResultMap(resultMap, cLog, cLog.getIsAdopt());
					} else if (aStudent.getDbmajorGradFacultyCode2().equals(cLog.getFacultyCode()) && "3".equals(cLog.getGetManner())
							&& NOT_ADOPT.equals(cLog.getIsAdopt())) {
						popMajor2 += Integer.parseInt(cLog.getCrd());// ���s�D�׾Ǩt
						GradUtil.putResultMap(resultMap, cLog, cLog.getIsAdopt());
					} else if (aStudent.getDbmajorGradFacultyCode1().equals(cLog.getFacultyCode()) && "3".equals(cLog.getGetManner())
							&& IS_ADOPT.equals(cLog.getIsAdopt())) {
						popAdopt1 += Integer.parseInt(cLog.getCrd());// ���s�ӽ��k��
						GradUtil.putResultMap(resultMap, cLog, cLog.getIsAdopt());
					} else if (aStudent.getDbmajorGradFacultyCode2().equals(cLog.getFacultyCode()) && "3".equals(cLog.getGetManner())
							&& IS_ADOPT.equals(cLog.getIsAdopt())) {
						popAdopt2 += Integer.parseInt(cLog.getCrd());// ���s�ӽ��k��
						GradUtil.putResultMap(resultMap, cLog, cLog.getIsAdopt());
					} else if (!aStudent.getDbmajorGradFacultyCode1().equals(cLog.getFacultyCode())
							&& !aStudent.getDbmajorGradFacultyCode2().equals(cLog.getFacultyCode()) && "3".equals(cLog.getGetManner())) {
						popOther += Integer.parseInt(cLog.getCrd());// ���s��L���
						GradUtil.putResultMap(resultMap, cLog, cLog.getIsAdopt());
					}
				}

				// �s�WLog
				Hashtable hashtable = transformer.poToHashtable(cLog);
				grat030Dao = new GRAT030DAO(dbManager, conn, hashtable, session);
				grat030Dao.insert();
				//109ccs check
				if( !graPassCcs109Yn && "2".equals(cLog.getGetManner()) && cLog.getGetAyear().compareTo(ParameterUtil.GRA_CCS_109) >= 0 ){
					graPassCcs109Yn = true;
				}
			}
		}

		// �ɹs
		GradUtil.setZeroResultMap(facultyCodeList, resultMap);

		
		//���~�Ѽ�
		GraParameters graParameters = (GraParameters) this.graParamHt.get(GradUtil.getGradKey(aStudent.getGradKind(), aStudent.getEdubkgrdAbility()));
		
		// ���׶W�L30�Ǥ��h�n�H30�Ǥ��p��(�t���פ��ĭp)
//		int summerAll = summer1 + summer2 + summerAdopt1 + summerAdopt2 + summerOther + summerCommon + summerCommonAdopt + summerWithoutAdopt;
//		if (summerAll > Integer.parseInt(parameters.getDoubleSummer())) {
//			summerAll = Integer.parseInt(parameters.getDoubleSummer());
//		}

		int total = adopt1 + adopt2 + major1 + major2 + summer1 + summer2 + summerAdopt1 + summerAdopt2 + summerOther + summerCommon + summerCommonAdopt
				+ other + common + commonAdopt + popAdopt1 + popAdopt2 + popMajor1 + popMajor2 + popCommonMajor + popCommonAdopt + popOther + remainCrd;

		// �B�z��׾Ǥ�
		int reduce = 0;
		if (reducedCrdMap.get(aStudent.getStno()) != null) {
			reduce = Integer.parseInt((String) reducedCrdMap.get(aStudent.getStno()));
		}

		// check�`�Ǥ�
		if (total + reduce < Integer.parseInt(parameters.getDoubleTotal())) {
			errorMsg.append(Reasons.TOTAL_NOT_ENOUGH);
			errorMsgCode.append(Reasons.TOTAL_NOT_ENOUGH_CODE);
		}		

		// check�D�׾Ǩt�@
		if ((adopt1 + major1 + popAdopt1 + popMajor1 + summer1 + summerAdopt1) < Integer.parseInt(parameters.getDoubleMajor1())) {
			errorMsg.append(Reasons.MAJOR_1_NOT_ENOUGH);
			errorMsgCode.append(Reasons.MAJOR_1_NOT_ENOUGH_CODE);

//			if (multiCrd > 0) {
//				errorMsgCode.append(Reasons.MAJOR_1_NOT_ENOUGH_CODE_M);
//			}
		}

		// check�D�׾Ǩt�G
		if ((adopt2 + major2 + popAdopt2 + popMajor2 + summer2 + summerAdopt2) < Integer.parseInt(parameters.getDoubleMajor2())) {
			errorMsg.append(Reasons.MAJOR_2_NOT_ENOUGH);
			errorMsgCode.append(Reasons.MAJOR_2_NOT_ENOUGH_CODE);

//			if (multiCrd > 0) {
//				errorMsgCode.append(Reasons.MAJOR_2_NOT_ENOUGH_CODE_N);
//			}
		}

		// check�ĭp�W��(�@�P�ҵ{���~)
		int pAdopt1 = GradUtil.getAdoptCrdParameter(adoptCrdList, aStudent, aStudent.getDbmajorGradFacultyCode1());// �ĭp�L�t�W��
		if (pAdopt1 != -1) {
			if ((adopt1 + popAdopt1 + summerAdopt1) > pAdopt1) {
				errorMsg.append(Reasons.ADOPT_OVER);
				errorMsgCode.append(Reasons.ADOPT_OVER_CODE);
			}
		}

		int pAdopt2 = GradUtil.getAdoptCrdParameter(adoptCrdList, aStudent, aStudent.getDbmajorGradFacultyCode2());// �ĭp�L�t�W��
		if (pAdopt2 != -1) {
			if ((adopt2 + popAdopt2 + summerAdopt2) > pAdopt2) {
				errorMsg.append(Reasons.ADOPT_OVER);
				errorMsgCode.append(Reasons.ADOPT_OVER_CODE);
			}
		}

		// check���פW��
		if ((summer1 + summer2 + summerOther + summerCommon + summerAdopt1 + summerAdopt2) > Integer.parseInt(parameters.getDoubleSummer())) {
			errorMsg.append(Reasons.SUMMER_OVER);
			errorMsgCode.append(Reasons.SUMMER_OVER_CODE);
		}

		// check���s�W��
		if ((popAdopt1 + popAdopt2 + popMajor1 + popMajor2 + popCommonMajor + popCommonAdopt + popOther) > Integer.parseInt(parameters.getDoublePop())) {
			errorMsg.append(Reasons.POP_OVER);
			errorMsgCode.append(Reasons.POP_OVER_CODE);
		}

		// check�D�׾Ǩt�@���խױo
		if (getCrd1 < Integer.parseInt(parameters.getDoubleOpen1())) {
			errorMsg.append(Reasons.CODE_1_NOT_ENOUGH);
			errorMsgCode.append(Reasons.CODE_1_NOT_ENOUGH_CODE);
		}

		// check�D�׾Ǩt�G���խױo
		if (getCrd2 < Integer.parseInt(parameters.getDoubleOpen2())) {
			errorMsg.append(Reasons.CODE_2_NOT_ENOUGH);
			errorMsgCode.append(Reasons.CODE_2_NOT_ENOUGH_CODE);
		}

		// �B�z���׬��		
		//if ("40".equals(aStudent.getDbmajorGradFacultyCode1()) || "40".equals(aStudent.getDbmajorGradFacultyCode2())) {
		//	if (old < 5 && (new1 < 3 || new2 < 3)) {
		//		// ���ŦX, �O�����q�L��]
		//		errorMsg.append(Reasons.REQUIRED_NOT_ENOUGH);
		//		errorMsgCode.append(Reasons.REQUIRED_NOT_ENOUGH_CODE);
		//	}
		//}
		//by poto �B�z����
		if(!checkObligatoryCrd(aStudent.getDbmajorGradFacultyCode1(),CrsnoList.toString())){
			if(!checkObligatoryCrd(aStudent.getDbmajorGradFacultyCode2(),CrsnoList.toString())){				
				errorMsg.append(Reasons.REQUIRED_NOT_ENOUGH);
				errorMsgCode.append(Reasons.REQUIRED_NOT_ENOUGH_CODE);
			}
		}
		
		//check�@�P�ҵ{		
		if ((common + commonAdopt + summerCommon + summerCommonAdopt + popCommonMajor + popCommonAdopt) < Integer.parseInt(graParameters.getCommon())) {
			errorMsg.append(Reasons.COMMON_NOT_ENOUGH);
			errorMsgCode.append(Reasons.COMMON_NOT_ENOUGH_CODE);
		}

		// check �q��
		String gelMsg = checkGelCrd(aStudent, this.gelVector, graParameters, CrsnoList.toString());
		if (!"".equals(gelMsg)) {
			dbManager.logger.append("no pass DISCIPLINE_CODE =" + gelMsg);
			errorMsg.append(Reasons.GENERAL_NOT_ENOUGH);
			errorMsgCode.append(Reasons.GENERAL_NOT_ENOUGH_CODE);
		}
		
		// ��o�Ǥ�
		if(passCrd<Integer.parseInt(this.parameters.getPassCrd())){
			errorMsg.append(Reasons.PASS_CRD_ENOUGH);
			errorMsgCode.append(Reasons.PASS_CRD_ENOUGH_CODE);
		}
		
		// ��o�Ǥ� + 109 css
		if (graPassCcs109Yn && (passCrd < Integer.parseInt(graParameters.getGraPassCcs109()))) {
			errorMsg.append(Reasons.PASS_CRD_ENOUGH);
			errorMsgCode.append(Reasons.PASS_CRD_ENOUGH_CODE);
		}

		// check�O�_�ŦX���~���
		Hashtable ht = new Hashtable();
		if ("".equals(errorMsg.toString().trim()) && "".equals(errorMsgCode.toString().trim())) {
			errorMsgCode.append(Reasons.AGREE_AUDIT_CODE);// �P�N�Ƽf
			if (FIRST_VERIFY.equals(status)) {
				ht.put("AUTO_AUDIT_STATUS", PASS);
				ht.put("AUTO_AUDIT_UNQUAL_CAUSE", "");
			} else if (LAST_VERIFY.equals(status)) {
				ht.put("GRAD_REEXAM_STATUS", PASS);
				ht.put("GRAD_REEXAM_UNQUAL_CAUSE", "");
				ht.put("GRADE_SCORE", DataGetter.getGrade(dbManager, conn, aStudent, SINGLE));// ���Z
				ht.put("GRAD_PROVE_NUMBER_1", GradUtil.getSenquence(dbManager, conn, session, aStudent.getAyear(), aStudent.getSms(), "1"));// ���~�ҮѸ�
				ht.put("GRAD_DATE", GradUtil.generateGradMonth("2", aStudent.getAyear(), aStudent.getSms()));// ���~�~��
			}
		} else {
			if (FIRST_VERIFY.equals(status)) {
				ht.put("AUTO_AUDIT_STATUS", NOT_PASS);
				ht.put("AUTO_AUDIT_UNQUAL_CAUSE", errorMsg.toString());
			} else if (LAST_VERIFY.equals(status)) {
				ht.put("GRAD_REEXAM_STATUS", NOT_PASS);
				ht.put("GRAD_REEXAM_UNQUAL_CAUSE", errorMsg.toString());
				ht.put("GRADE_SCORE", DataGetter.getGrade(dbManager, conn, aStudent, SINGLE));// ���Z
				ht.put("GRAD_PROVE_NUMBER_1", "");// ���~�ҮѸ�
				ht.put("GRAD_DATE", "");// ���~�~��
			}
		}
		
		
		// �s��@�P�ҵ{
		resultMap.put("90", new Integer((common + summerCommon + popCommonMajor)));
		resultMap.put("90" + ADOPT_NAME, new Integer((commonAdopt + popCommonAdopt + summerCommonAdopt)));

		// �Ngrat003�����A�אּ�q�L�Τ��q�L�A�å[�W���q�L��]
		StringBuffer sql = new StringBuffer();
		sql.append("AYEAR = '" + aStudent.getAyear() + "' ");
		sql.append("AND SMS = '" + aStudent.getSms() + "' ");
		sql.append("AND STNO = '" + aStudent.getStno() + "' ");

		GRAT003DAO grat003Dao = new GRAT003DAO(dbManager, conn, ht, session);
		grat003Dao.update(sql.toString());
		
		//by poto
		ht = new Hashtable();
		ht.put("GRADE_SCORE", DataGetter.getGrade(dbManager, conn, aStudent, SINGLE));// ���Z
		grat003Dao = new GRAT003DAO(dbManager, conn, ht, session);		
		grat003Dao.update(sql.toString());

		// ���~�q����B�Ƽf�ԲӸ��(ONE)
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

		// ���~�q����B�Ƽf�ԲӸ��_�U�Ǩt�Ǥ��P�ĭp�Ǥ�(MULTI)
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

		// ���~�q����B�Ƽf�ԲӸ��_�@�P������Ǥ�(MULTI)
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
		// ���o�ǥͭױo���Ҧ����
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

				//by poto �B�z����
				if(CrsnoList.length()!=0){					
					CrsnoList.append(","+ci.getCrsno());
				}else{					
					CrsnoList.append(ci.getCrsno());
				}
				
				// �P�_�O�_�����׽ҵ{
				String key = ci.getAyear() + ci.getSms() + ci.getCrsno();
				// if (!IS_SUMMER.equals(DataGetter.isSummer(dbManager, conn,
				// aStudent.getStno(), ci.getCrsno()))) {
				if ("1".equals(ci.getSms()) || "2".equals(ci.getSms()) || "2".equals(ci.getGetManner()) || summerMap.get(key) != null) {
					// �O������k�ݾǨt��LOG
					CheckingLog cLog = new CheckingLog();
					cLog.setAyear(aStudent.getAyear());// �ӽоǦ~
					cLog.setSms(aStudent.getSms());// �ӽоǴ�
					cLog.setStno(ci.getStno());
					cLog.setKind(status);
					cLog.setFacultyCode(ci.getFacultyCode());
					cLog.setCrsno(ci.getCrsno());
					cLog.setCrd(this.getCrsnoNewCrd(ci, cLog.getFacultyCode()));
					cLog.setGetAyear(ci.getAyear());// ��ب��o�Ǧ~
					cLog.setGetSms(ci.getSms());// ��ب��o�Ǵ�

					// �P�_�O�_����Ǧ~���ת���(�o�䤣�|�����ת��ҵ{)
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

					// �ۦP�ʽ���
					if (IS_VALID.equals(cLog.getIsValid())) {
						String tempKey = aStudent.getStno() + cLog.getGetAyear() + cLog.getGetSms() + cLog.getCrsno() + cLog.getGetManner();
						if (this.crsnoMap.get(tempKey) != null) {
							cLog.setIsValid(NOT_VALID);
						}
					}

					// �P�_��جO�_���h����������
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

		// ���o�ǥʹ��׻P�ĭp���Ҧ����
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

				// �O������k�ݾǨt��LOG
				CheckingLog cLog = new CheckingLog();
				cLog.setAyear(aStudent.getAyear());// �ӽоǦ~
				cLog.setSms(aStudent.getSms());// �ӽоǴ�
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
				cLog.setGetAyear(ca.getGetAyear());// ��ب��o�Ǧ~
				cLog.setGetSms(ca.getGetSms());// ��ب��o�Ǵ�

				// �P�_�O�_����Ǧ~���ת���
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

				// �P�_��جO�_����
				if (GradUtil.isCrsnoValid(deadlineList, cLog)) {
					cLog.setIsValid(IS_VALID);
				} else {
					cLog.setIsValid(NOT_VALID);
				}

				// �P�_���׽ҵ{�O�_���D�׾Ǩt
				if (isSummerMajor) {
					cLog.setIsAdopt(NOT_ADOPT);
				} else {
					// 20090106�s�W, ��t�@�}��cout103�٬O�|�]�w���@�D�h�ħΦ�, ���ѭ��W�٬O�|�O����t�Ҭ��D�}
					// ��Gra002m�ӽЮɭY�M�w�N�Ӭ�ĭp���L���, �o��|�P�_��t�@�}, �ñN��אּ�D�׾Ǩt
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

				// �ۦP�ʽ���
				if (IS_VALID.equals(cLog.getIsValid())) {
					String tempKey = aStudent.getStno() + cLog.getGetAyear() + cLog.getGetSms() + cLog.getCrsno() + cLog.getGetManner();
					if (this.crsnoMap.get(tempKey) != null) {
						cLog.setIsValid(NOT_VALID);
					}
				}

				// �P�_��جO�_���h����������
				MultiCrsno mc = GradUtil.isMultiCrsno(multiCrsnoList, aStudent, cLog);
				if (mc == null) {
					logList.add(cLog);
				} else {
					cLog.setMc(mc);
					mList.add(cLog);
				}
			}
		}

		// �N�h���������ة�N�Ǥ�����إ[��logList
		remainCrd += GradUtil.processMultiCrsno(mList, logList);

		// �}�l�p��Ǥ�
		int adopt = 0;// �ӽ��k��
		int major = 0;// �D�׾Ǩt
		int summer = 0;// ���ץD�׾Ǩt
		int summerAdopt = 0;// ���ץӽ��k��
		int summerOther = 0;// ���ר�L���
		int summerCommon = 0;// ���צ@�P�ҵ{
		int summerCommonAdopt = 0;// ���צ@�P�ҵ{�ӽ��k��
		int popAdopt = 0;// ���s�ӽ��k��
		int popMajor = 0;// ���s�D�׾Ǩt
		int popCommonMajor = 0;// ���s�@�P�ҵ{�D�׾Ǩt
		int popCommonAdopt = 0;// ���s�@�P�ҵ{�ӽ��k��
		int popOther = 0;// ���s��L���
		int other = 0;// ��L���
		int common = 0;// �@�P�ҵ{
		int commonAdopt = 0;// �@�P�ҵ{�ӽ��k��
		Map resultMap = new HashMap();
		Map commonMap = new HashMap();

		// 20090220�s�W
		int getCrd1 = 0;// �D�׾Ǩt�@���խױo
		int getCrd2 = 0;// �D�׾Ǩt�G���խױo

		// �R��Log
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

				// ��ئ��Įɤ~�p��Ǥ�
				if (IS_VALID.equals(cLog.getIsValid()) || IS_MULTI_CRSNO_NCRD.equals(cLog.getIsValid()) || IS_MULTI_CRSNO_PART.equals(cLog.getIsValid())) {
					// 20090220�s�W, �p�⥻�խױo�Ǥ�
					if (aStudent.getGradMajorFaculty().equals(cLog.getFacultyCode()) && !"2".equals(cLog.getGetManner())) {
						getCrd2 += Integer.parseInt(cLog.getCrd());
					} else if (facultyCodeKind04.equals(cLog.getFacultyCode()) && !"2".equals(cLog.getGetManner())) {
						getCrd1 += Integer.parseInt(cLog.getCrd());
					}

					if ("90".equals(cLog.getFacultyCode()) && NOT_ADOPT.equals(cLog.getIsAdopt())
							&& ("1".equals(cLog.getGetManner()) || "2".equals(cLog.getGetManner()))) {
						common += Integer.parseInt(cLog.getCrd());// �@�P�ҵ{
						GradUtil.putCommonMap(commonKind, commonMap, cLog, COMMON_PRINT_4);
					} else if ("90".equals(cLog.getFacultyCode()) && IS_ADOPT.equals(cLog.getIsAdopt())
							&& ("1".equals(cLog.getGetManner()) || "2".equals(cLog.getGetManner()))) {
						commonAdopt += Integer.parseInt(cLog.getCrd());// �@�P�ҵ{�ӽ��k��
						GradUtil.putCommonMap(commonKind, commonMap, cLog, COMMON_PRINT_4);
					} else if (aStudent.getGradMajorFaculty().equals(cLog.getFacultyCode()) && NOT_ADOPT.equals(cLog.getIsAdopt())
							&& ("1".equals(cLog.getGetManner()) || "2".equals(cLog.getGetManner()))) {
						major += Integer.parseInt(cLog.getCrd());// �D�׾Ǩt
						GradUtil.putResultMap(resultMap, cLog, cLog.getIsAdopt());
					} else if (aStudent.getGradMajorFaculty().equals(cLog.getFacultyCode()) && IS_ADOPT.equals(cLog.getIsAdopt())
							&& ("1".equals(cLog.getGetManner()) || "2".equals(cLog.getGetManner()))) {
						adopt += Integer.parseInt(cLog.getCrd());// �ӽ��k��
						GradUtil.putResultMap(resultMap, cLog, cLog.getIsAdopt());
					} else if (!aStudent.getGradMajorFaculty().equals(cLog.getFacultyCode())
							&& ("1".equals(cLog.getGetManner()) || "2".equals(cLog.getGetManner()))) {
						other += Integer.parseInt(cLog.getCrd());// ��L���
						GradUtil.putResultMap(resultMap, cLog, cLog.getIsAdopt());
					} else if ("90".equals(cLog.getFacultyCode()) && TO_SUMMER.equals(cLog.getGetManner()) && NOT_ADOPT.equals(cLog.getIsAdopt())) {
						summerCommon += Integer.parseInt(cLog.getCrd());// ���צ@�P�ҵ{
						GradUtil.putCommonMap(commonKind, commonMap, cLog, COMMON_PRINT_4);
					} else if ("90".equals(cLog.getFacultyCode()) && TO_SUMMER.equals(cLog.getGetManner()) && IS_ADOPT.equals(cLog.getIsAdopt())) {
						summerCommonAdopt += Integer.parseInt(cLog.getCrd());// ���צ@�P�ҵ{�ӽ��k��
						GradUtil.putCommonMap(commonKind, commonMap, cLog, COMMON_PRINT_4);
					} else if (aStudent.getGradMajorFaculty().equals(cLog.getFacultyCode()) && TO_SUMMER.equals(cLog.getGetManner())
							&& NOT_ADOPT.equals(cLog.getIsAdopt())) {
						summer += Integer.parseInt(cLog.getCrd());// ���ץD�׾Ǩt
						GradUtil.putResultMap(resultMap, cLog, cLog.getIsAdopt());
					} else if (aStudent.getGradMajorFaculty().equals(cLog.getFacultyCode()) && TO_SUMMER.equals(cLog.getGetManner())
							&& IS_ADOPT.equals(cLog.getIsAdopt())) {
						summerAdopt += Integer.parseInt(cLog.getCrd());// ���ץӽ��k��
						GradUtil.putResultMap(resultMap, cLog, cLog.getIsAdopt());
					} else if (!aStudent.getGradMajorFaculty().equals(cLog.getFacultyCode()) && TO_SUMMER.equals(cLog.getGetManner())) {
						summerOther += Integer.parseInt(cLog.getCrd());// ���ר�L���
						GradUtil.putResultMap(resultMap, cLog, cLog.getIsAdopt());
					} else if ("90".equals(cLog.getFacultyCode()) && "3".equals(cLog.getGetManner()) && NOT_ADOPT.equals(cLog.getIsAdopt())) {
						popCommonMajor += Integer.parseInt(cLog.getCrd());// ���s�@�P�ҵ{�D�׾Ǩt
						GradUtil.putCommonMap(commonKind, commonMap, cLog, COMMON_PRINT_4);
					} else if ("90".equals(cLog.getFacultyCode()) && "3".equals(cLog.getGetManner()) && IS_ADOPT.equals(cLog.getIsAdopt())) {
						popCommonAdopt += Integer.parseInt(cLog.getCrd());// ���s�@�P�ҵ{�ӽ��k��
						GradUtil.putCommonMap(commonKind, commonMap, cLog, COMMON_PRINT_4);
					} else if (aStudent.getGradMajorFaculty().equals(cLog.getFacultyCode()) && "3".equals(cLog.getGetManner())
							&& NOT_ADOPT.equals(cLog.getIsAdopt())) {
						popMajor += Integer.parseInt(cLog.getCrd());// ���s�D�׾Ǩt
						GradUtil.putResultMap(resultMap, cLog, cLog.getIsAdopt());
					} else if (aStudent.getGradMajorFaculty().equals(cLog.getFacultyCode()) && "3".equals(cLog.getGetManner())
							&& IS_ADOPT.equals(cLog.getIsAdopt())) {
						popAdopt += Integer.parseInt(cLog.getCrd());// ���s�ӽ��k��
						GradUtil.putResultMap(resultMap, cLog, cLog.getIsAdopt());
					} else if (!aStudent.getGradMajorFaculty().equals(cLog.getFacultyCode()) && "3".equals(cLog.getGetManner())) {
						popOther += Integer.parseInt(cLog.getCrd());// ���s��L���
						GradUtil.putResultMap(resultMap, cLog, cLog.getIsAdopt());
					}
				}

				// �s�WLog
				Hashtable hashtable = transformer.poToHashtable(cLog);
				grat030Dao = new GRAT030DAO(dbManager, conn, hashtable, session);
				grat030Dao.insert();
			}
		}

		// �ɹs
		GradUtil.setZeroResultMap(facultyCodeList, resultMap);
		
		int total = adopt + major + summer + summerAdopt + summerOther + summerCommon + summerCommonAdopt + other + common + commonAdopt + popAdopt + popMajor + popCommonMajor + popCommonAdopt + popOther + remainCrd;

		// �B�z��׾Ǥ�
		int reduce = 0;
		if (reducedCrdMap.get(aStudent.getStno()) != null) {
			reduce = Integer.parseInt((String) reducedCrdMap.get(aStudent.getStno()));
		}

		// check�`�Ǥ�
		if (total + reduce < Integer.parseInt(parameters.getTotal())) {
			errorMsg.append(Reasons.TOTAL_NOT_ENOUGH);
			errorMsgCode.append(Reasons.TOTAL_NOT_ENOUGH_CODE);
		}

		// check�@�P�ҵ{(�¨����D�׵L���ˬd������)
		// �B�z�q�� (�¨����D�׵L���ˬd������)
		
		// check�D�׾Ǩt
		if ((adopt + major + summer + summerAdopt + popAdopt + popMajor) < Integer.parseInt(parameters.getMajor())) {
			errorMsg.append(Reasons.MAJOR_05_NOT_ENOUGH);
			errorMsgCode.append(Reasons.MAJOR_05_NOT_ENOUGH_CODE);

			if ("841618917".equals(aStudent.getStno())) {
				errorMsgCode.append(Reasons.MAJOR_2_NOT_ENOUGH_CODE_N);
			}
		}

		// check�ĭp�W��
		int pAdopt = GradUtil.getAdoptCrdParameter(adoptCrdList, aStudent, aStudent.getGradMajorFaculty());// �ĭp�L�t�W��
		if (pAdopt != -1) {
			if ((adopt + summerAdopt + popAdopt) > pAdopt) {
				errorMsg.append(Reasons.ADOPT_OVER);
				errorMsgCode.append(Reasons.ADOPT_OVER_CODE);
			}
		}

		// check���פW��
		if ((summer + summerAdopt + summerOther + summerCommon + summerCommonAdopt) > Integer.parseInt(parameters.getSummer())) {
			errorMsg.append(Reasons.SUMMER_OVER);
			errorMsgCode.append(Reasons.SUMMER_OVER_CODE);
		}

		// check���s�W��
		if ((popAdopt + popMajor + popCommonMajor + popCommonAdopt + popOther) > Integer.parseInt(parameters.getPop())) {
			errorMsg.append(Reasons.POP_OVER);
			errorMsgCode.append(Reasons.POP_OVER_CODE);
		}

		// check�D�׾Ǩt�@���խױo
		if (getCrd1 < Integer.parseInt(parameters.getDoubleOpen1())) {
			errorMsg.append(Reasons.CODE_1_NOT_ENOUGH);
			errorMsgCode.append(Reasons.CODE_1_NOT_ENOUGH_CODE);
		}

		// check�D�׾Ǩt�G���խױo
		if (getCrd2 < Integer.parseInt(parameters.getDoubleOpen2())) {
			errorMsg.append(Reasons.CODE_2_NOT_ENOUGH);
			errorMsgCode.append(Reasons.CODE_2_NOT_ENOUGH_CODE);
		}
		
		//by poto �B�z����
		if(!checkObligatoryCrd(aStudent.getGradMajorFaculty(),CrsnoList.toString())){
			errorMsg.append(Reasons.REQUIRED_NOT_ENOUGH);
			errorMsgCode.append(Reasons.REQUIRED_NOT_ENOUGH_CODE);
		}
		
		
		//�ױo�Ǥ���
		if (passCrd < Integer.parseInt(this.parameters.getPassCrd())) {
			errorMsg.append(Reasons.PASS_CRD_ENOUGH);
			errorMsgCode.append(Reasons.PASS_CRD_ENOUGH_CODE);
		}

		// check�O�_�ŦX���~���
		Hashtable ht = new Hashtable();
		if ("".equals(errorMsg.toString().trim()) && "".equals(errorMsgCode.toString().trim())) {
			errorMsgCode.append(Reasons.AGREE_AUDIT_CODE);// �P�N�Ƽf
			if (FIRST_VERIFY.equals(status)) {
				ht.put("AUTO_AUDIT_STATUS", PASS);
				ht.put("AUTO_AUDIT_UNQUAL_CAUSE", "");
			} else if (LAST_VERIFY.equals(status)) {
				ht.put("GRAD_REEXAM_STATUS", PASS);
				ht.put("GRAD_REEXAM_UNQUAL_CAUSE", "");
				ht.put("GRADE_SCORE", DataGetter.getGrade(dbManager, conn, aStudent, OLD_DOUBLE));// ���Z
				ht.put("GRAD_PROVE_NUMBER_1", GradUtil.getSenquence(dbManager, conn, session, aStudent.getAyear(), aStudent.getSms(), "1"));// ���~�ҮѸ�
				ht.put("GRAD_DATE", GradUtil.generateGradMonth("2", aStudent.getAyear(), aStudent.getSms()));// ���~�~��
			}
		} else {
			if (FIRST_VERIFY.equals(status)) {
				ht.put("AUTO_AUDIT_STATUS", NOT_PASS);
				ht.put("AUTO_AUDIT_UNQUAL_CAUSE", errorMsg.toString());
			} else if (LAST_VERIFY.equals(status)) {
				ht.put("GRAD_REEXAM_STATUS", NOT_PASS);
				ht.put("GRAD_REEXAM_UNQUAL_CAUSE", errorMsg.toString());
				ht.put("GRADE_SCORE", DataGetter.getGrade(dbManager, conn, aStudent, OLD_DOUBLE));// ���Z
				ht.put("GRAD_PROVE_NUMBER_1", "");// ���~�ҮѸ�
				ht.put("GRAD_DATE", "");// ���~�~��
			}
		}

		// �s��@�P�ҵ{
		resultMap.put("90", new Integer((common + summerCommon + popCommonMajor)));
		resultMap.put("90" + ADOPT_NAME, new Integer((commonAdopt + popCommonAdopt + summerCommonAdopt)));

		// �Ngrat003�����A�אּ�q�L�Τ��q�L�A�å[�W���q�L��]
		StringBuffer sql = new StringBuffer();
		sql.append("AYEAR = '" + aStudent.getAyear() + "' ");
		sql.append("AND SMS = '" + aStudent.getSms() + "' ");
		sql.append("AND STNO = '" + aStudent.getStno() + "' ");

		GRAT003DAO grat003Dao = new GRAT003DAO(dbManager, conn, ht, session);
		grat003Dao.update(sql.toString());
		
		//by poto
		ht = new Hashtable();
		ht.put("GRADE_SCORE", DataGetter.getGrade(dbManager, conn, aStudent, OLD_DOUBLE));// ���Z
		grat003Dao = new GRAT003DAO(dbManager, conn, ht, session);		
		grat003Dao.update(sql.toString());
		
		// ���~�q����B�Ƽf�ԲӸ��(ONE)
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

		// ���~�q����B�Ƽf�ԲӸ��_�U�Ǩt�Ǥ��P�ĭp�Ǥ�(MULTI)
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

		// ���~�q����B�Ƽf�ԲӸ��_�@�P������Ǥ�(MULTI)
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

	// ���o��طs���Ǥ���
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

	// ���o��طs���Ǥ���
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
	 * new�q�ѽҵ{�ˬd
	 * */
	private String checkGelCrd(ApplyStudents aStudent, Vector vtMain, GraParameters graParameters, String crsnoList) throws Exception {
		StringBuffer msg = new StringBuffer();
		// i 2_����y��
		// j 3_�~��y��
		// k 4_��T���i
		// 1 5_�H��P���N
		// 2 6_���|�P�k�v
		// 3 7_���d�P����
		// X 8_�q�ѱШ|���y
		try {
			// ��K���
			String gelCrsnoList = ParameterUtil.getGelReduce(dbManager, conn, aStudent.getStno());

			// �p��Ҧ��������Ǥ���
			Hashtable crdHt = new Hashtable();
			for (int k = 0; k < vtMain.size(); k++) {
				String DISCIPLINE_CODE = "";
				int crdSum = 0;
				Vector vt = (Vector) vtMain.get(k);
				// �C�@�� DISCIPLINE_CODE
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

			// ���y��111�W�Ǵ��ҧ�אּ�֤߽ҵ{
			//if (Integer.parseInt(graParameters.getCourse()) > this.getGelCrd(crdHt, new String[] { ParameterUtil.DISCIPLINE_CODE_8 })) {
			//	msg.append(ParameterUtil.DISCIPLINE_CODE_8 + ",");
			//}

			// ���y+�֤�
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
	 * �q��
	 * */
	private int getGelCrd(Hashtable crdHt, String[] codeAry) {
		int crd = 0;
		for (int i = 0; i < codeAry.length; i++) {
			crd += Integer.parseInt(Utility.nullToSpace(crdHt.get(codeAry[i])));
		}
		return crd;
	}
}
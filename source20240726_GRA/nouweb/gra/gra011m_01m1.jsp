<%/*
----------------------------------------------------------------------------------
File Name        : gra011m_01m1.jsp
Author            : sorge
Description        : GRA011M_�n�����~��f���A - �B�z�޿譶��
Modification Log    :

Vers        Date           By                Notes
--------------    --------------    --------------    ----------------------------------
0.0.1        097/01/21    sorge        Code Generate Create
----------------------------------------------------------------------------------
*/%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="MS950"%>
<%@ include file="/utility/header.jsp"%>
<%@ include file="/utility/modulepageinit.jsp"%>
<%@ page import="com.nou.gra.dao.*"%>
<%@ page import="com.nou.cou.dao.*"%>
<%@ page import="com.nou.gra.bo.*"%>
<%@ page import="com.nou.gra.bo.queuejob.*"%>

<%!
public int getfacindex(String[] faculty, String fac)
{	
	int ret = -1;
	System.out.println(faculty.length);
	for(int i=0;i<faculty.length;i++)
	{
		System.out.println(faculty[i] + " " + fac);
		if(faculty[i].equals(fac))
			ret = i;
	}
	return ret;
}

public int[] getCRD(DBManager dbManager, Hashtable requestMap, HttpSession session) throws Exception
{
	int[] crd = new int[3];
	try
	{
		Connection conn       =    dbManager.getConnection(AUTCONNECT.mapConnect("GRA", session));
		
		GRAT001DAO	GRAT001 = new GRAT001DAO(dbManager, conn);
		GRAT001.setResultColumn("ASYS, FTSTUD_INNER_RQRCRD, POP_CRD_F, POP_CRD_D ");
		GRAT001.setAYEAR(Utility.dbStr(requestMap.get("AYEAR")));
		GRAT001.setSMS(Utility.dbStr(requestMap.get("SMS")));
		//GRAT001.setASYS("1");
		DBResult rs = GRAT001.query();
		
		while(rs.next())
		{
			crd[0] = rs.getInt("FTSTUD_INNER_RQRCRD");
			crd[1] = rs.getInt("POP_CRD_F");
			crd[2] = rs.getInt("POP_CRD_D");
		}
	}
	catch (Exception ex)
    {
        throw ex;
    }
    finally
    {
    }
	return crd;
}

public int[][] getFACULTY_CRD(DBManager dbManager, Hashtable requestMap, HttpSession session, String[] faculty) throws Exception
{
	int[][] crd = null;
	try
	{
		Connection conn       =    dbManager.getConnection(AUTCONNECT.mapConnect("GRA", session));
		
		COUT102DAO	COUT102 = new COUT102DAO(dbManager, conn);
		COUT102.setResultColumn("COUNT(DISTINCT FACULTY_CODE) AS CNT ");
		COUT102.setWhere("TOTAL_CRS_NO='01' AND CRS_GROUP_CODE IN ('002', '003')");
		DBResult rs = COUT102.query();
		int tot=0;
		if(rs.next())
			tot = rs.getInt("CNT");
			
		COUT102.setResultColumn("FACULTY_CODE, CRS_GROUP_CODE, CRS_GROUP_CRD ");
		COUT102.setOrderbyColumn("FACULTY_CODE, CRS_GROUP_CODE ");
		rs = COUT102.query();		
		
		//faculty = new String[tot];
		crd = new int[tot][2];
		int loc=-1;
		String pFACULTY_CODE ="";
		
		while(rs.next())
		{
			if( !pFACULTY_CODE.equals(rs.getString("FACULTY_CODE")) )
			{
				pFACULTY_CODE = rs.getString("FACULTY_CODE");
				loc++;
				faculty[loc] = rs.getString("FACULTY_CODE");
				crd[loc][0] = 0;
				crd[loc][1] = 0;
			}
			
			int group = rs.getInt("CRS_GROUP_CODE")-2;
			
			crd[loc][group] = rs.getInt("CRS_GROUP_CRD");
		}
	}
	catch (Exception ex)
    {
        throw ex;
    }
    finally
    {
    }
	return crd;
}

/** �B�z�d�� Grid ��� */
public void doQuery(JspWriter out, DBManager dbManager, Hashtable requestMap, HttpSession session) throws Exception
{
    Connection conn = null;

    try
    {
        int        pageNo        =    Integer.parseInt(Utility.checkNull(requestMap.get("pageNo"), "1"));
        int        pageSize      =    Integer.parseInt(Utility.checkNull(requestMap.get("pageSize"), "10"));

        conn       =    dbManager.getConnection(AUTCONNECT.mapConnect("GRA", session));
			
        GRAT003GATEWAY GRAT003 = new GRAT003GATEWAY(dbManager, conn, pageNo, pageSize);
		Vector vt = new Vector();
		GRAT003.getGra011mQuery(vt, requestMap);
		out.println(DataToJson.vtToJson ( GRAT003.getTotalRowCount(), vt));
    }
    catch (Exception ex)
    {
        throw ex;
    }
    finally
    {
        dbManager.close();
        if (conn != null)
            conn.close();

        conn    =    null;
    }
}

public void doCHK(JspWriter out, DBManager dbManager, Hashtable requestMap, HttpSession session) throws Exception
{
    try
    {
		Connection conn       =    dbManager.getConnection(AUTCONNECT.mapConnect("GRA", session));
		
		//�ھ�GRAT003���٥��f�ֳq�L���ӽСA�A�h��GRAT009���L���
		//�N�bGRAT009�L��ƪ��ӽСA�]��VECTOR�b����CHK���Ƶ{��
		GRAT003GATEWAY GRAT003 = new GRAT003GATEWAY(dbManager, conn);
		Vector vt = new Vector();
		GRAT003.getGra011mChkData(vt, requestMap);

		//���o�o�Ǧ~���A�Ťj���~�ݭn�צ@�P��ت��Ǥ��A���ץͱ��s�ĭp�Ǥ��W�u�A���D�ץͱ��s�ĭp�Ǥ��W�u
		int[] CRD = getCRD(dbManager, requestMap, session);
		
		String[] faculty = new String[20];
		for(int i=0;i<20;i++)
			faculty[i]="";
		//���o�o�Ǵ��A�U�t���Y�D�}�P�ĭp�L�t�Ǥ����]�w
		int[][] FACULTY_CRD = getFACULTY_CRD(dbManager, requestMap, session, faculty);
		
		//�qGRAT009��X�o���n�h�q���f�d����ơA�۰ʱư��w�g�f�ֹL�����
		Vector vtData = new Vector();
		GRAT003.getGra011mGetChkData(vtData, requestMap);

		//�̫�|�o��n�A
		for(int i=0;i<vtData.size();i++)
		{
			Hashtable data = (Hashtable)vtData.get(i);

			String STNO = (String)data.get("STNO");
			String ASYS = (String)data.get("ASYS");
			String IDNO = (String)data.get("IDNO");
			String FACULTY_CODE = (String)data.get("FACULTY_CODE");
			String BEFORE_AYEAR_SMS = (String)data.get("BEFORE_AYEAR_SMS");
			
			int tot = 2;
			if( FACULTY_CODE.length() != 9 )
				tot++;

			//�A�h�Ǥ��Ȧ�d�ݳo��ǥͿ�ת��Ǥ��ƬO�_�ŦX�з�
			Vector vtCRD = new Vector();
			GRAT003.getGra011mGetCRD(vtCRD, FACULTY_CODE, ASYS, STNO, BEFORE_AYEAR_SMS);
			
			//��z���
			int[][] stdata = new int[tot][2];
			String[] fac = new String[tot];
			String pFACULTY_CODE ="";
			int loc = -1;
			int pop_crd = 0; //���s�Ǥ�
			for(int j=0;j<vtCRD.size();j++)
			{
				Hashtable crddata = (Hashtable)vtCRD.get(j);

				if( !pFACULTY_CODE.equals((String)crddata.get("FACULTY_CODE")) )
				{
					pFACULTY_CODE = (String)crddata.get("FACULTY_CODE");
					loc++;
					stdata[loc][0] = 0;
					stdata[loc][1] = 0;
					fac[loc] = (String)crddata.get("FACULTY_CODE");
				}

				int CRS_GROUP_CODE = Integer.parseInt((String)crddata.get("CRS_GROUP_CODE"))-2;
				
				//���O�p�⥻�t�D�}�P�ĭp�L�t�Ǥ�
				stdata[loc][CRS_GROUP_CODE] += Integer.parseInt((String)crddata.get("CRD"));
				
				//���s�Ш|�Ǥ��֭p
				if( "3".equals((String)crddata.get("GET_MANNER")) )
					pop_crd += Integer.parseInt((String)crddata.get("CRD"));
			}
			
			int crdflag=0;
			String AUTO_AUDIT_STATUS = "3";
			//�@�P��جO�_���W�L
			if( CRD[0] <= (stdata[tot-1][0]+stdata[tot-1][1]) )
			{
				System.out.println(STNO+" �@�P��ؾǤ����F");
				
				for(int j=0;j<tot-1;j++)
				{
					int facindex = getfacindex(faculty, fac[j]);
					System.out.println(stdata[j][1]+" "+FACULTY_CRD[facindex][1]);
					if( stdata[j][0] >= FACULTY_CRD[facindex][0] && stdata[j][1] <= FACULTY_CRD[facindex][1] )
					{
						System.out.println(STNO+" �Ǥ�����OK");
						crdflag = 1;
					}
					else
					{
						System.out.println(STNO+" �Ǥ����礣OK");
						crdflag = 0;
					}
				}
				
				if( crdflag == 1 )
				{
					if(CRD[tot-1] >= pop_crd)
					{
						System.out.println(STNO+" ���s�Ш|�Ǥ��S���W�L");
						AUTO_AUDIT_STATUS = "2";
					}
					else
					{
						System.out.println(STNO+" ���s�Ш|�Ǥ����W�L");
					}
				}
			}
			else
			{
				System.out.println(STNO+" �@�P��ؾǤ�����");
			}
			
			Hashtable ht = new Hashtable();
			ht.put("AUTO_AUDIT_STATUS", AUTO_AUDIT_STATUS);
			GRAT003DAO	GRAT003IN = new GRAT003DAO(dbManager, conn, ht, session);
			GRAT003IN.update("AYEAR = '" + Utility.dbStr(requestMap.get("AYEAR")) + "' AND SMS = '" + Utility.dbStr(requestMap.get("SMS")) + "' AND STNO = '" + STNO + "' AND IDNO = '" + IDNO + "' ");
		}
		
		dbManager.commit();
		
		out.println(DataToJson.successJson());
	}
    catch (Exception ex)
    {
        throw ex;
    }
    finally
    {
        dbManager.close();
    }
}

public void doGetNAME(JspWriter out, DBManager dbManager, Hashtable requestMap, HttpSession session) throws Exception
{
	try
	{
		Connection	conn		=	dbManager.getConnection(AUTCONNECT.mapConnect("GRA", session));

		GRAT014GATEWAY GRAT014 = new GRAT014GATEWAY(dbManager, conn);
		Vector vt = GRAT014.getGra026mSTData(requestMap);

		out.println(DataToJson.vtToJson (vt));
	}
	catch (Exception ex)
	{
		/** Rollback Transaction */
		dbManager.rollback();

		throw ex;
	}
	finally
	{
		dbManager.close();
	}
}

public void doGetBaseData(JspWriter out, DBManager dbManager, Hashtable requestMap, HttpSession session) throws Exception
{
	try
	{
		Connection	conn		=	dbManager.getConnection(AUTCONNECT.mapConnect("GRA", session));

		GRAT003GATEWAY GRAT003 = new GRAT003GATEWAY(dbManager, conn);
		Vector vt = new Vector();
		GRAT003.getGra011mQuery(vt, requestMap);
		
		out.println(DataToJson.vtToJson (vt));
	}
	catch (Exception ex)
	{
		/** Rollback Transaction */
		dbManager.rollback();

		throw ex;
	}
	finally
	{
		dbManager.close();
	}
}

public void doGetGrat029(JspWriter out, DBManager dbManager, Hashtable requestMap, HttpSession session) throws Exception
{
	try
	{
		Connection	conn		=	dbManager.getConnection(AUTCONNECT.mapConnect("GRA", session));

		GRAT029DAO	GRAT029 = new GRAT029DAO(dbManager, conn);
		GRAT029.setResultColumn("FACULTY_CODE, (SELECT FACULTY_NAME FROM SYST003 WHERE FACULTY_CODE=GRAT029.FACULTY_CODE AND ASYS='1') AS FACULTY_NAME, FACULTY_TOTAL, ADOPT_TOTAL ");
		GRAT029.setAYEAR(Utility.dbStr(requestMap.get("AYEAR")));
		GRAT029.setSMS(Utility.dbStr(requestMap.get("SMS")));
		GRAT029.setSTNO(Utility.dbStr(requestMap.get("STNO")));
		GRAT029.setKIND(Utility.dbStr(requestMap.get("KIND")));
		GRAT029.setOrderbyColumn("FACULTY_CODE ");
		DBResult rs = GRAT029.query();
		
		out.println(DataToJson.rsToJson(GRAT029.getTotalRowCount(), rs));
	}
	catch (Exception ex)
	{
		/** Rollback Transaction */
		dbManager.rollback();

		throw ex;
	}
	finally
	{
		dbManager.close();
	}
}

public void doGetGrat031(JspWriter out, DBManager dbManager, Hashtable requestMap, HttpSession session) throws Exception
{
	try
	{
		Connection	conn		=	dbManager.getConnection(AUTCONNECT.mapConnect("GRA", session));

		GRAT031GATEWAY GRAT031 = new GRAT031GATEWAY(dbManager, conn);
		Vector vt = GRAT031.getGra011mGetGRAT031Data(requestMap);
		
		out.println(DataToJson.vtToJson(vt));
	}
	catch (Exception ex)
	{
		/** Rollback Transaction */
		dbManager.rollback();

		throw ex;
	}
	finally
	{
		dbManager.close();
	}
}

public void doGetGrat028(JspWriter out, DBManager dbManager, Hashtable requestMap, HttpSession session) throws Exception
{
	try
	{
		Connection	conn		=	dbManager.getConnection(AUTCONNECT.mapConnect("GRA", session));

		//�qGRAT028���o�f�ֹL���@�ǼƦr
		GRAT028DAO	GRAT028 = new GRAT028DAO(dbManager, conn);
		GRAT028.setResultColumn
		(
				"TOTAL, POP, SUMMER, REDUCE, RESULT, SUMMER_WITHOUT_ADOPT, TOTAL+REDUCE+SUMMER_WITHOUT_ADOPT AS ALLCRD "+
				",(select A.RMK from grat047 A  " +
				"  JOIN STUT003 B ON B.GRAD_KIND = A.GRAD_KIND AND B.EDUBKGRD_ABILITY = A.EDUBKGRD_ABILITY  " +
				"  WHERE A.ayear = '"+Utility.dbStr(requestMap.get("AYEAR"))+"' AND A.SMS = '"+Utility.dbStr(requestMap.get("SMS"))+"' AND B.STNO = '"+Utility.dbStr(requestMap.get("STNO"))+"' )   AS RMK "
		);
		GRAT028.setAYEAR(Utility.dbStr(requestMap.get("AYEAR")));
		GRAT028.setSMS(Utility.dbStr(requestMap.get("SMS")));
		GRAT028.setSTNO(Utility.dbStr(requestMap.get("STNO")));
		GRAT028.setKIND(Utility.dbStr(requestMap.get("KIND")));
		DBResult rs = GRAT028.query();

		Vector vt = new Vector();
		
		Hashtable rowHt = null;
		if (rs.next())
		{
			rowHt = new Hashtable();
			for (int i = 1; i <= rs.getColumnCount(); i++)
				rowHt.put(rs.getColumnName(i), rs.getString(i));
			rowHt.put("MARK", "1");
			vt.add(rowHt);
		}
		
		//���o�o�Ǧ~�������~�Ѽ�
		GRAT001DAO	GRAT001 = new GRAT001DAO(dbManager, conn);
		GRAT001.setResultColumn("MAJOR, SUMMER, POP, DOUBLE_MAJOR_1, DOUBLE_MAJOR_2, DOUBLE_SUMMER, DOUBLE_POP ");
		GRAT001.setAYEAR(Utility.dbStr(requestMap.get("AYEAR")));
		GRAT001.setSMS(Utility.dbStr(requestMap.get("SMS")));
		rs = GRAT001.query();
		
		if (rs.next())
		{
			rowHt = new Hashtable();
			for (int i = 1; i <= rs.getColumnCount(); i++)
				rowHt.put(rs.getColumnName(i), rs.getString(i));
			rowHt.put("MARK", "2");
			
		}
		String msg = "";
		if( !"".equals(Utility.dbStr(requestMap.get("FACULTY_CODE"))) )
		{
			//���o���׽ҵ{
			GRAT007DAO	GRAT007 = new GRAT007DAO(dbManager, conn);
			GRAT007.setResultColumn("FACULTY_CODE, (SELECT FACULTY_NAME FROM SYST003 WHERE SYST003.FACULTY_CODE=GRAT007.FACULTY_CODE AND SYST003.ASYS='1') AS FACULTY_NAME, CRSNO, (SELECT CRS_NAME FROM COUT002 WHERE COUT002.CRSNO=GRAT007.CRSNO) AS CRS_NAME ");
			GRAT007.setWhere("IS_SEND_OUT='Y' AND FACULTY_CODE IN ("+Utility.dbStr(requestMap.get("FACULTY_CODE"))+") AND AYEAR='"+Utility.dbStr(requestMap.get("AYEAR"))+"' AND SMS='"+Utility.dbStr(requestMap.get("SMS"))+"' ");
			GRAT007.setOrderbyColumn("FACULTY_CODE, CRSNO ");
			rs = GRAT007.query();

			String fac = "";
			int s = 0;
			while(rs.next())
			{
				if( !fac.equals(rs.getString("FACULTY_CODE")) )
				{
					fac = rs.getString("FACULTY_CODE");
					msg += "<br>"+rs.getString("FACULTY_NAME")+"���׬��<br>�@";
					s=0;
				}
				if(s!=0)
					msg += "�B";
					
				msg += rs.getString("CRS_NAME");
				s++;
			}
		}
		rowHt.put("MUST", msg);
		
		vt.add(rowHt);
		out.println(DataToJson.vtToJson(vt));
	}
	catch (Exception ex)
	{
		/** Rollback Transaction */
		dbManager.rollback();

		throw ex;
	}
	finally
	{
		dbManager.close();
	}
}

/** �B�z�C�L�\�� */
private void doPrint(JspWriter out, DBManager dbManager, Hashtable requestMap, HttpSession session) throws Exception
{
	try
	{
		Connection	conn	=	dbManager.getConnection(AUTCONNECT.mapConnect("PER", session));
				
		//�ұ��C�L�����
		
		// ��l�� rptFile /
		RptFile		rptFile	=	new RptFile(session.getId());
		String reportName = "gra011m_01r1";
		rptFile.setColumn("��_1,��_2,��_3,��_4,��_5,��_6,��_7,��_8");

		GRAT030DAO	GRAT030 = new GRAT030DAO(dbManager, conn);
		GRAT030.setResultColumn
		(
			"STNO, GROUP_CODE, FACULTY_CODE, " +
			"CRSNO, (SELECT CRS_NAME FROM COUT002 WHERE COUT002.CRSNO=GRAT030.CRSNO) AS CRS_NAME, " +
			"GET_AYEAR, GET_SMS, CRD, " +
			"DECODE(GET_MANNER,'1','��','2','��','3','��','4','��','5', '��') AS GET_MANNER, IS_VALID "
		);
		
		GRAT030.setAYEAR(Utility.dbStr(requestMap.get("AYEAR")));
		GRAT030.setSMS(Utility.dbStr(requestMap.get("SMS")));
		GRAT030.setSTNO(Utility.dbStr(requestMap.get("STNO")));
		GRAT030.setKIND(Utility.dbStr(requestMap.get("KIND")));
		GRAT030.setOrderbyColumn("GROUP_CODE, FACULTY_CODE, CRSNO ");
		DBResult rs = GRAT030.query();
	
		String GROUP_CODE = "";
		String FACULTY_CODE = "";
		int s=0;
		int crd=0;
	
		//���p��U���ߪ��H��
		while(rs.next())
		{
			if( s==0 )
			{
				GROUP_CODE = rs.getString("GROUP_CODE");
				FACULTY_CODE = rs.getString("FACULTY_CODE");
			}
			
			if( s!=0 && (!GROUP_CODE.equals(rs.getString("GROUP_CODE")) || !FACULTY_CODE.equals(rs.getString("FACULTY_CODE"))))
			{
				GROUP_CODE = rs.getString("GROUP_CODE");
				FACULTY_CODE = rs.getString("FACULTY_CODE");
				
				//�X�p�Ǥ�
				rptFile.add("#000000");
				rptFile.add("");
				rptFile.add("");
				rptFile.add("");
				rptFile.add("�X�p�Ǥ�");
				rptFile.add(crd);
				rptFile.add("");
				rptFile.add("");
				
				rptFile.add("#000000");
				rptFile.add("");
				rptFile.add("");
				rptFile.add("");
				rptFile.add("");
				rptFile.add("");
				rptFile.add("");
				rptFile.add("");
				
				crd = 0;
			}
		
			String IS_VALID = rs.getString("IS_VALID");
			if("N".equals(IS_VALID)){
				rptFile.add("#FF0000");
			}else if("B".equals(IS_VALID)){
				rptFile.add("#0000FF");
			}else if("G".equals(IS_VALID)){
				rptFile.add("#00FF00");
			}else{
				rptFile.add("#000000");
			}
			
			rptFile.add(rs.getString("STNO"));
			rptFile.add("grow= "+rs.getString("GROUP_CODE"));
			rptFile.add("dep= "+rs.getString("FACULTY_CODE"));
			rptFile.add(rs.getString("CRSNO")+"-"+rs.getString("GET_AYEAR")+rs.getString("GET_SMS"));
			rptFile.add(rs.getString("CRD"));
			rptFile.add(rs.getString("GET_MANNER"));
			rptFile.add(rs.getString("CRS_NAME"));
			
			if("Y".equals(IS_VALID) || "B".equals(IS_VALID) || "G".equals(IS_VALID))
				crd += rs.getInt("CRD");
			
			s++;
		}	
		
		//�X�p�Ǥ�
		rptFile.add("#000000");
		rptFile.add("");
		rptFile.add("");
		rptFile.add("");
		rptFile.add("�X�p�Ǥ�");
		rptFile.add(crd);
		rptFile.add("");
		rptFile.add("");
		
		if (rptFile.size() == 0 || s==0)
		{
			out.println("<script>top.close(); alert(\"�L�ŦX��ƥi�ѦC�L!!\");window.close();</script>");
			return;
		}
		
		Hashtable	ht	=	new Hashtable();
		
		// ��l�Ƴ����� /
		report		report_	=	new report(dbManager, conn, out, reportName, report.onlineHtmlMode);
		
		report_.setDynamicVariable(ht);

		// �}�l�C�L /
		report_.genReport(rptFile);
	}
	catch (Exception ex)
	{
		throw ex;
	}
	finally
	{
		dbManager.close();
	}
}

public void doProgress(JspWriter out, DBManager dbManager, Hashtable requestMap, HttpSession session) throws Exception {
	
	try{
		Integer cInt = (Integer)session.getAttribute("check_total_count");
		Integer cCur = (Integer)session.getAttribute("check_current_count");
		if(cInt != null && cCur != null){
			int cTotal = cInt.intValue();
			int cCurrent = cCur.intValue();
		
			Hashtable ht = new Hashtable();
			ht.put("progress", String.valueOf(cCurrent) + "/" + String.valueOf(cTotal));
	
			out.println(DataToJson.htToJson(ht));
		} else {
			Hashtable ht = new Hashtable();
			ht.put("progress", "");
			out.println(DataToJson.htToJson(ht));
		}
	}
	catch (Exception ex)
	{
		throw ex;
	}
	finally
	{
		dbManager.close();
	}
}

//�q����f
public void doCheck(JspWriter out, DBManager dbManager, Hashtable requestMap, HttpSession session) throws Exception {
	session.setAttribute("check_kind", new Integer(1));
	CheckScheduler.start(out, dbManager, requestMap, session);
	out.println(DataToJson.successJson());
}

//�q����f(�浧)
public void doCheckOne(JspWriter out, DBManager dbManager, Hashtable requestMap, HttpSession session) throws Exception {
	Checker checker = new FirstCheckInOneRecord(out, dbManager, requestMap, session);
	checker.run();
}

//�q���Ƽf
public void doCheckFinal(JspWriter out, DBManager dbManager, Hashtable requestMap, HttpSession session) throws Exception {
	session.setAttribute("check_kind", new Integer(2));
	CheckScheduler.start(out, dbManager, requestMap, session);
	
	out.println(DataToJson.successJson());
}

//�q���Ƽf(�浧)
public void doCheckFinalOne(JspWriter out, DBManager dbManager, Hashtable requestMap, HttpSession session) throws Exception {
	Checker checker = new LastCheckInOneRecord(out, dbManager, requestMap, session);
	checker.run();
}

public void doCheckDIPLOMA_SEQ(JspWriter out, DBManager dbManager, Hashtable requestMap, HttpSession session) throws Exception
{
	try
	{
		Connection conn	= dbManager.getConnection(AUTCONNECT.mapConnect("GRA", session));
		String ayear = (String) requestMap.get("AYEAR");
		String sms = (String) requestMap.get("SMS");
		GRAT003DAO	GRAT003 = new GRAT003DAO(dbManager, conn);
		GRAT003.setResultColumn(" count(1) AS NUM ");
		GRAT003.setWhere(" ayear = '"+ayear+"' and sms ='"+sms+"' AND GRAD_PROVE_NUMBER_1 is not null ");
		DBResult rs = GRAT003.query();

		out.println(DataToJson.rsToJson(rs));
	}
	catch (Exception ex)
	{
		/** Rollback Transaction */
		dbManager.rollback();

		throw ex;
	}
	finally
	{
		dbManager.close();
	}
}

// �Ƽf�e���ˮ�
public void doReCheck(JspWriter out, DBManager dbManager, Hashtable requestMap, HttpSession session) throws Exception
{
	Connection conn	= null;
	DBResult rs = null;
	try
	{
		conn	= dbManager.getConnection(AUTCONNECT.mapConnect("GRA", session));
		//	1.�Ǩt�f�֧�����I(grat003 ��Ǧ~�� COMMON_SEND_OUT='Y' AND GRAD_SEND_OUT='Y'),�ˮ֬O�_�����Ŧ�����
		GRAT003DAO grat003 = new GRAT003DAO(dbManager, conn);
		grat003.setResultColumn("1");
		grat003.setWhere("AYEAR='"+requestMap.get("AYEAR")+"' AND SMS='"+requestMap.get("SMS")+"' AND (COMMON_SEND_OUT<>'Y' OR GRAD_SEND_OUT<>'Y') "+(requestMap.get("CLICK_BTN_NAME").toString().equals("RT3")?"AND STNO='"+requestMap.get("STNO")+"' ":"")+"AND ROWNUM=1");
		rs = grat003.query();
		
		String result = "";
		if(rs.next())
			result = "�Ǩt�f�֩|����I";
		rs.close();
		
		// ���~�ҮѸ��]�w����(grat024 ��Ǧ~�� kind=101),�Y�L�X�{�T���u�Ǩt�f�֩|����I, ���~�ҮѸ��|���]�w�v
		GRAT024DAO grat024 = new GRAT024DAO(dbManager, conn);
		grat024.setResultColumn("1");
		grat024.setWhere("ASYS='1' AND AYEAR='"+requestMap.get("AYEAR")+"' AND SMS='"+requestMap.get("SMS")+"' AND KIND='101' AND ROWNUM=1 ");
		rs = grat024.query();
		
		if(!rs.next())
			result += (result.equals("")?"":"\n")+"���~�ҮѸ��|���]�w";
		rs.close();
		
		out.println(DataToJson.successJson(result));
	}
	catch (Exception ex)
	{
		/** Rollback Transaction */
		dbManager.rollback();

		throw ex;
	}
	finally
	{
		if(rs!=null)
			rs.close();
		if(conn!=null)
			conn.close();
		if(dbManager!=null)
			dbManager.close();
	}
}

// ��f�e���ˮ�
public void doInitCheck(JspWriter out, DBManager dbManager, Hashtable requestMap, HttpSession session) throws Exception
{
	Connection conn	= null;
	DBResult rs = null;
	try
	{
		conn	= dbManager.getConnection(AUTCONNECT.mapConnect("GRA", session));
		//	1.�ե����ѼƳ]�w����(grat001 ��Ǧ~�������) �Y�L�X�{�T���u���~�ѼƩ|���]�w�v
		GRAT001DAO grat001 = new GRAT001DAO(dbManager, conn);
		grat001.setResultColumn("1");
		grat001.setWhere("AYEAR='"+requestMap.get("AYEAR")+"' AND SMS='"+requestMap.get("SMS")+"'");
		rs = grat001.query();
		
		String result = "";
		if(!rs.next())
			result = "���~�ѼƩ|���]�w";
		rs.close();		
		
		out.println(DataToJson.successJson(result));
	}
	catch (Exception ex)
	{
		/** Rollback Transaction */
		dbManager.rollback();

		throw ex;
	}
	finally
	{
		if(rs!=null)
			rs.close();
		if(conn!=null)
			conn.close();
		if(dbManager!=null)
			dbManager.close();
	}
}

%>

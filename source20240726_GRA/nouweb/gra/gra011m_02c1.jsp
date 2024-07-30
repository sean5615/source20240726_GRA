<%/*
----------------------------------------------------------------------------------
File Name		: gra011m_02c1
Author			: sorge
Description		: gra011m_�ǥ͸պ�t�� - �s�豱��� (javascript)
Modification Log	:

Vers		Date       	By            	Notes
--------------	--------------	--------------	----------------------------------
0.0.1		097/03/06	sorge    	Code Generate Create
----------------------------------------------------------------------------------
*/%>
<%@ page contentType="text/html; charset=UTF-8" errorPage="/utility/errorpage.jsp" pageEncoding="MS950"%>
<%@ include file="/utility/header.jsp"%>
<%@ include file="/utility/jspageinit.jsp"%>

/** �פJ javqascript Class */
doImport ("Query.js, ErrorHandle.js, LoadingBar_0_2.js, Form.js, Ajax_0_2.js, ArrayUtil.js, ReSize.js, SortTable.js");

/** ��l�]�w������T */
var	currPage		=	"<%=request.getRequestURI()%>";
var	printPage		=	"gra011m_01p1.jsp";	//�C�L����
var	editMode		=	"ADD";				//�s��Ҧ�, ADD - �s�W, MOD - �ק�
var	_privateMessageTime	=	-1;				//�T����ܮɶ�(���ۭq�� -1)
var	controlPage		=	"gra011m_01c2.jsp";	//�����
var	queryObj		=	new queryObj();			//�d�ߤ���
var fac1 = "";
var fac2 = "";

/** ������l�� */
function page_init()
{
	page_init_start_2();

	/** === ��l���]�w === */
	/** ��l�s����� */

	Form.iniFormColor();
	
	page_init_end_2();
}


/** ============================= ���ץ��{����m�� ======================================= */
/** �]�w�\���v�� */
function securityCheck()
{
	try
	{
		/** �s�W */
		if (!<%=AUTICFM.securityCheck (session, "ADD")%>)
		{
			noPermissAry[noPermissAry.length]	=	"ADD";
			editMode	=	"NONE";
			try{Form.iniFormSet("EDIT", "ADD_BTN", "D", 1);}catch(ex){}
		}
		/** �ק� */
		if (!<%=AUTICFM.securityCheck (session, "UPD")%>)
		{
			noPermissAry[noPermissAry.length]	=	"UPD";
		}
		/** �s�W�έק� */
		if (!chkSecure("ADD") && !chkSecure("UPD"))
		{
			try{Form.iniFormSet("EDIT", "SAVE_BTN", "D", 1);}catch(ex){}
		}
		/** �R�� */
		if (!<%=AUTICFM.securityCheck (session, "DEL")%>)
		{
			noPermissAry[noPermissAry.length]	=	"DEL";
			try{Form.iniFormSet("RESULT", "DEL_BTN", "D", 1);}catch(ex){}
		}
		/** �ץX */
		if (<%=AUTICFM.securityCheck (session, "EXP")%>)
		{
			noPermissAry[noPermissAry.length]	=	"EXP";
			try{Form.iniFormSet("RESULT", "EXPORT_BTN", "D", 1);}catch(ex){}
			try{Form.iniFormSet("QUERY", "EXPORT_ALL_BTN", "D", 1);}catch(ex){}
		}
		/** �C�L */
		if (!<%=AUTICFM.securityCheck (session, "PRT")%>)
		{
			noPermissAry[noPermissAry.length]	=	"PRT";
			try{Form.iniFormSet("RESULT", "PRT_BTN", "D", 1);}catch(ex){}
			try{Form.iniFormSet("QUERY", "PRT_ALL_BTN", "D", 1);}catch(ex){}
		}
	}
	catch (ex)
	{
	}
}

/** �ˬd�v�� - ���v��/�L�v��(true/false) */
function chkSecure(secureType)
{
	if (noPermissAry.toString().indexOf(secureType) != -1)
		return false;
	else
		return true
}
/** ====================================================================================== */

function setItem(AYEAR, SMS, STNO, KIND)
{
	Form.setInput('EDIT', 'KIND', KIND);
	Form.setInput('EDIT', 'AYEAR', AYEAR);
	Form.setInput('EDIT', 'SMS', SMS);
	Form.setInput('EDIT', 'STNO', STNO);
	
	//doQuery();
	doGetBaseData();
	doGetGrat029();
	doGetGrat031();
	doGetGrat028();
}

function doGetBaseData()
{
	var	callBack	=	function doGetBaseData.callBack(ajaxData)
	{
		var    sms = new Array("", "�W�Ǵ�", "�U�Ǵ�", "����");
		var    type = new Array("", "��D�ײ��~", "���D�ײ��~", "", "", "�¨����D�ײ��~");
		
		fac1 = (ajaxData.data[0].DBMAJOR_GRAD_FACULTY_NAME1 != "" ? ajaxData.data[0].DBMAJOR_GRAD_FACULTY_NAME1 : ajaxData.data[0].GRAD_MAJOR_FACULTY_NAME);
		fac2 = (ajaxData.data[0].DBMAJOR_GRAD_FACULTY_NAME2 != "" ? ajaxData.data[0].DBMAJOR_GRAD_FACULTY_NAME2 : "");
		
		Form.setInput('EDIT', 'FACULTY_CODE', ajaxData.data[0].FACULTY_CODE);
		
		BASEDATA.innerHTML = "" +
			"<table width='100%' cellspacing='0' cellpadding='5' border='1'>" +
			"	<tr><td align='right' class='tdgl1'>�Ǧ~���G</td><td align='left'>" + ajaxData.data[0].AYEAR+sms[ajaxData.data[0].SMS] + "</td></tr>" +
			"	<tr><td align='right' class='tdgl2'>�Ǹ��G</td><td align='left'>" + ajaxData.data[0].STNO + "</td></tr>" +
			"	<tr><td align='right' class='tdgl1'>�m�W�G</td><td align='left'>" + ajaxData.data[0].NAME + "</td></tr>" +
			"	<tr><td align='right' class='tdgl2'>���ߡG</td><td align='left'>" + ajaxData.data[0].CENTER_NAME + "</td></tr>" +
			"	<tr><td align='right' class='tdgl2'>���O�G</td><td align='left'>" + type[parseInt(ajaxData.data[0].APP_GRAD_TYPE)] + "</td></tr>" +
			"	<tr><td align='right' class='tdgl2'>�Ǩt�@�G</td><td align='left'>" + fac1 + "&nbsp;</td></tr>" +
			"	<tr><td align='right' class='tdgl2'>�Ǩt�G�G</td><td align='left'>" + fac2 + "&nbsp;</td></tr>" +
			"</table>";
	}
	sendFormData("EDIT", controlPage, "GET_BASE_DATA_MODE", callBack, "999");
}

function doGetGrat029()
{
	var	callBack	=	function doGetGrat029.callBack(ajaxData)
	{
		var html = "";
		
		if (ajaxData == null)
            return;
		
		html = "" +
			"<table width='100%' cellspacing='0' cellpadding='5' border='1'>" +
			"	<tr class='mtbGreenBg'> " +
			"		<td width=20>&nbsp;</td>" +
			"		<td resize='on' nowrap>�}�]</td>" +
			"		<td resize='on' nowrap>�ĭp</td>" +
			"	</tr>";
			
		var faculty = "";	
		for (var i = 0; i < ajaxData.data.length; i++)
		{
			if( fac1 == ajaxData.data[i].FACULTY_NAME )
			{
				fac1 = (ajaxData.data[i].FACULTY_TOTAL*1+ajaxData.data[i].ADOPT_TOTAL*1);
				faculty = ajaxData.data[i].FACULTY_CODE;
			}
			if( fac2 == ajaxData.data[i].FACULTY_NAME )
			{
				fac2 = (ajaxData.data[i].FACULTY_TOTAL*1+ajaxData.data[i].ADOPT_TOTAL*1);	
				faculty += ","+ajaxData.data[i].FACULTY_CODE;
			}
			
			html += "" +
				"	<tr class='listColor0"+(i%2)+"'> " +
				"		<td>�@" + ajaxData.data[i].FACULTY_NAME.substr(0,1) + "</td>" +
				"		<td align='right'>" + (ajaxData.data[i].FACULTY_TOTAL*1+ajaxData.data[i].ADOPT_TOTAL*1) + "&nbsp;</td>" +
				"		<td align='right'>" + ajaxData.data[i].ADOPT_TOTAL + "&nbsp;</td>" +
				"	</tr>";
		}		
		
		if (ajaxData.data.length == 0)
		{
			html += "" +
				"	<tr class='listColor00'> " +
				"		<td colspan='3'><font color=red><b>�@�@�@�d�L�}�]�ĭp���!!</b></font></td>" +
				"	</tr>";
		}
		
		html += "</table>";
		Form.setInput('EDIT', 'FACULTY_CODE', faculty);
		
		GRAT029.innerHTML = html;
	}
	sendFormData("EDIT", controlPage, "GET_GRAT029_DATA_MODE", callBack, "999");
}

function doGetGrat031()
{
	var	callBack	=	function doGetGrat031.callBack(ajaxData)
	{
		if (ajaxData == null)
            return;
	
		html = "" +
			"<table width='50%' cellspacing='0' cellpadding='5' border='1'>";

		var total = 0;
		
		for (var i = 0; i < ajaxData.data.length; i++)
		{
			html += "" +
				"	<tr> " +
				"		<td>&nbsp;</td>" +
				"		<td align='right'>" + ajaxData.data[i].NAME + ( ajaxData.data[i].CRD=="" ? "" : "(�� "+ajaxData.data[i].CRD+")") + "�G</td>" +
				"		<td align='right'>" + (ajaxData.data[i].TOTAL_CRD=="" ? "0" : ajaxData.data[i].TOTAL_CRD) + "&nbsp;</td>" +
				"	</tr>";
			
			total += parseInt(ajaxData.data[i].TOTAL_CRD=="" ? "0" : ajaxData.data[i].TOTAL_CRD);
		}
		
		html += "" +
				"	<tr> " +
				"		<td>&nbsp;</td>" +
				"		<td align='right'>�@�P��X�p�G</td>" +
				"		<td align='right'>" + total + "&nbsp;</td>" +
				"	</tr>";
		
		html += "</table>";
		
		GRAT031.innerHTML = html;
	}
	sendFormData("EDIT", controlPage, "GET_GRAT031_DATA_MODE", callBack, "999");
}

function doGetGrat028()
{
	var	callBack	=	function doGetGrat028.callBack(ajaxData)
	{
		if (ajaxData == null)
            return;
			
		if (ajaxData.data[0].MARK == "2")	
		{
			GRAT028.innerHTML = "<font color=red><br><b>�@�@�@�٥��f��</b></font>";
			FINAL_RESULT.innerHTML = "";
			return;
		}
		else
		{
			html = "<br>�@���~����G"+ajaxData.data[0].RMK+"<br>";
			//html += "<br>�@�Ǩt�@(��"+(fac2==""? ajaxData.data[1].MAJOR_TOTAL : ajaxData.data[1].DOUBLE_MAJOR_1)+")�G "+fac1+"<br>"; //2024/7/27����
			html += "<br>�@�D�׾Ǩt�G "+fac1+"<br>";
			if( fac2 != "" )
				html += "�@�Ǩt�G(��"+ajaxData.data[1].DOUBLE_MAJOR_2+")�G "+fac2+"<br>";
			
			html += "<br>�@���~�`�Ǥ�(�ĭp"+ajaxData.data[0].TOTAL+"+���"+ajaxData.data[0].REDUCE+") +������"+ajaxData.data[0].SUMMER_WITHOUT_ADOPT+"=��o"+ajaxData.data[0].ALLCRD+"<br>";
			
			html += "<br>�@���s�Ш|�Ǥ���+�i�Үv�Ǥ���(<="+(fac2==""? ajaxData.data[1].POP : ajaxData.data[1].DOUBLE_POP)+")�G"+ajaxData.data[0].POP;
			html += "<br>�@�����}���ת̥~�A������رĭp�Ǥ���(<="+(fac2==""? ajaxData.data[1].SUMMER : ajaxData.data[1].DOUBLE_SUMMER)+")�G"+ajaxData.data[0].SUMMER;
			
			html += "<br>"+ajaxData.data[1].MUST;
			
			GRAT028.innerHTML = html;
			
			FINAL_RESULT.innerHTML = (Form.getInput('EDIT', 'KIND')=="1" ? "��f" : "�Ƽf")+"���G�G"+ajaxData.data[0].RESULT;
			EditStatus.innerHTML = (Form.getInput('EDIT', 'KIND')=="1" ? "��f" : "�Ƽf")+"�f�ֵ��G";
		}
	}
	sendFormData("EDIT", controlPage, "GET_GRAT028_DATA_MODE", callBack, "999");
}

function doPrintLog()
{
	var	printWin	=	WindowUtil.openPrintWindow("", "Print");
	Form.doSubmit("EDIT", printPage, "post", "Print");
	printWin.focus();	
}
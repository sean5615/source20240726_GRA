package com.nou.gra.bo;


import java.sql.Connection;
import java.util.*;


import com.acer.db.DBManager;
import com.acer.db.query.DBResult;
import com.acer.util.Utility;
import com.acer.file.RptFile;
import com.nou.UtilityX;
import com.nou.gra.bo.print.CrdAdoptSql;
import com.nou.gra.dao.GRAT003DAO;
import com.nou.gra.dao.GRAT009DAO;
import com.nou.gra.dao.GRAT004DAO;
import com.nou.gra.po.CrdAdoptCrd;
import com.nou.gra.po.CrdAdoptPrinter;
import com.nou.stu.dao.STUT003GATEWAY;


public class CrdAdopt {
	private Hashtable ht;

	public CrdAdopt(Hashtable ht){
		this.ht = ht;
	}

	/**
	 * 取得減修學分
	 *
	 * @param dbManager
	 * @param conn
	 * @return
	 * @throws Exception
	 */
	public Map getReduceCrd(DBManager dbManager, Connection conn, Hashtable requestMap) throws Exception {
		String ayear = (String) requestMap.get("AYEAR");
		String sms = (String) requestMap.get("SMS");
		String stno = (String) requestMap.get("STNO");

		StringBuffer sql = new StringBuffer();

		sql.append("select a.stno, a.accum_reduce_crd                                \n");
		sql.append("from stut003 a                                                   \n");
		sql.append("join grat003 b on a.stno = b.stno                                \n");
		sql.append("where b.ayear = '" + ayear + "' and b.sms = '" + sms + "' and a.accum_reduce_crd > 0 \n");

		if (stno != null && !"".equals(stno)) {
			sql.append("and a.stno = '" + stno + "'                                         \n");
		}

		DBResult rs = null;
		Map map = new LinkedHashMap();
		try {
			rs = dbManager.getSimpleResultSet(conn);
			rs.open();
			rs.executeQuery(sql.toString());

			while (rs.next()) {
				map.put(rs.getString("stno"), rs.getString("accum_reduce_crd"));
			}
		} catch (Exception e) {
			throw e;
		} finally {
			if (rs != null)
				rs.close();
		}

		return map;
	}
	
	/**
	 * 取得當學期選課學分
	 *
	 * @param dbManager
	 * @param conn
	 * @return
	 * @throws Exception
	 */ 
	public String getRegCrd(DBManager dbManager, Connection conn,String ayear,String sms,String stno) throws Exception {
		String crd = "0";
		StringBuffer sql = new StringBuffer();
		sql.append("SELECT SUM(NVL(B.CRD,'0')) AS SUMCRD ");
		sql.append("FROM REGT007 A  ");
		sql.append("JOIN COUT002 B ON A.CRSNO = B.CRSNO ");
		sql.append("WHERE 1=1  ");
		sql.append("AND A.STNO = '"+stno+"' AND A.AYEAR = '"+ayear+"' AND A.SMS ='"+sms+"'  ");  
		sql.append("AND A.UNQUAL_TAKE_MK = 'N' AND A.UNTAKECRS_MK ='N' AND A.PAYMENT_STATUS != '1'  ");    
		DBResult rs = null;		
		try {
			rs = dbManager.getSimpleResultSet(conn);
			rs.open();
			rs.executeQuery(sql.toString());
			if (rs.next()) {
				crd = rs.getString("SUMCRD"); 
			}
		} catch (Exception e) {
			throw e;
		} finally {
			if (rs != null)
				rs.close();
		}
		return crd;
	}
	
	public Hashtable getCcst003Crd(DBManager dbManager, Connection conn, String STNO) throws Exception {
		
		StringBuffer sql = new StringBuffer();
		sql.append("SELECT ");
		sql.append("NVL(SUM(NVL(REPL_CRD,'0')),'0') AS REPL_CRD_SUM, ");
		sql.append("NVL(SUM(NVL(ADOPT_CRD,'0')),'0') AS ADOPT_CRD_SUM, ");
		sql.append("NVL(SUM(NVL(REDUCE_CRD,'0')),'0') AS REDUCE_CRD_SUM, ");
		sql.append("NVL(SUM(NVL(REPL_CRD,'0')+NVL(ADOPT_CRD,'0')),'0') AS TOTAL_SUM ");
		sql.append("FROM CCST003  ");
		sql.append("WHERE 1=1  ");
		sql.append("AND STNO = '"+STNO+"'  ");
		sql.append("AND TRIM(OLD_STNO) IS NULL  ");                                           

		DBResult rs = null;
		Hashtable map = new Hashtable();
		try {
			rs = dbManager.getSimpleResultSet(conn);
			rs.open();
			rs.executeQuery(sql.toString());
			while (rs.next()) {
				for (int i = 1; i <= rs.getColumnCount(); i++){
					map.put(rs.getColumnName(i), rs.getString(i));
				}    
			}
		} catch (Exception e) {
			throw e;
		} finally {
			if (rs != null)
				rs.close();
		}

		return map;
	}
	
	/**
	 * 取得小計學分
	 *
	 * @param dbManager
	 * @param conn
	 * @return
	 * @throws Exception
	 */
	public Map getMajorCrd(DBManager dbManager, Connection conn, Hashtable requestMap, String stno, String dep1, String dep2) throws Exception {
		String ayear = (String) requestMap.get("AYEAR");
		String sms = (String) requestMap.get("SMS");

		StringBuffer sql = new StringBuffer();

		sql.append("select nvl((                                              \n");
		sql.append("    select sum(a.crd) as total                            \n");
		sql.append("    from grat004 a                                        \n");
		sql.append("    where a.stno = '" + stno + "' and a.adopt_faculty = '" + dep1 + "' \n");
		sql.append("    and a.ayear = '" + ayear + "' and a.sms = '" + sms + "'                   \n");
		sql.append("    group by a.stno                                       \n");
		sql.append("), 0) as dep1_crd, nvl((                                  \n");
		sql.append("    select sum(a.crd) as total                            \n");
		sql.append("    from grat004 a                                        \n");
		sql.append("    where a.stno = '" + stno + "' and a.adopt_faculty = '" + dep2 + "' \n");
		sql.append("    and a.ayear = '" + ayear + "' and a.sms = '" + sms + "'                   \n");
		sql.append("    group by a.stno                                       \n");
		sql.append("), 0) as dep2_crd, nvl((                                  \n");
		sql.append("    select sum(a.crd) as total                            \n");
		sql.append("    from grat004 a                                        \n");
		sql.append("    where a.stno = '" + stno + "' and a.adopt_faculty = '90' \n");
		sql.append("    and a.ayear = '" + ayear + "' and a.sms = '" + sms + "'                   \n");
		sql.append("    group by a.stno                                       \n");
		sql.append("), 0) as common                                           \n");
		sql.append("from dual                                                 \n");

		DBResult rs = null;
		Map map = new LinkedHashMap();
		try {
			rs = dbManager.getSimpleResultSet(conn);
			rs.open();
			rs.executeQuery(sql.toString());

			if (rs.next()) {
				map.put("1", rs.getString("dep1_crd"));
				map.put("2", rs.getString("dep2_crd"));
				map.put("3", rs.getString("common"));
			} else {
				map.put("1", "0");
				map.put("2", "0");
				map.put("3", "0");
			}
		} catch (Exception e) {
			throw e;
		} finally {
			if (rs != null)
				rs.close();
		}

		return map;
	}

	/**
	 * 取得小計學分(舊制雙主修)
	 *
	 * @param dbManager
	 * @param conn
	 * @return
	 * @throws Exception
	 */
	public Map getMajorCrdForKind05(DBManager dbManager, Connection conn, Hashtable requestMap, String stno, String dep1, String dep2) throws Exception {
		String ayear = (String) requestMap.get("AYEAR");
		String sms = (String) requestMap.get("SMS");

		StringBuffer sql = new StringBuffer();

		sql.append("select nvl((                                              \n");
		sql.append("  select sum(crd) as total from (                                                                  \n");
		sql.append("    select b.* from (                                                                            \n");
		sql.append("        select *                                                                                 \n");
		sql.append("        from grat003 x                                                                           \n");
		sql.append("        where x.app_grad_type = '04' and x.num_of_times = (                                      \n");
		sql.append("            select a.num_of_times from grat003 a                                                 \n");
		sql.append("            where a.ayear = '" + ayear + "' and a.sms = '" + sms + "' and a.app_grad_type = '05' and x.stno = a.stno \n");
		sql.append("        ) and x.grad_reexam_status = '2'                                                         \n");
		sql.append("    ) a join grat004 b on a.ayear = b.ayear and a.sms = b.sms and a.stno = b.stno                \n");
		sql.append("    where a.stno = '" + stno + "' and b.adopt_faculty = '" + dep1 + "'                                        \n");
		sql.append("    union                                                                                        \n");
		sql.append("    select b.* from grat003 a                                                                    \n");
		sql.append("    join grat004 b on a.ayear = b.ayear and a.sms = b.sms and a.stno = b.stno                    \n");
		sql.append("    where b.ayear = '" + ayear + "' and b.sms = '" + sms + "' and a.app_grad_type = '05'                             \n");
		sql.append("    and a.stno = '" + stno + "' and b.adopt_faculty = '" + dep1 + "'                                             \n");
		sql.append("  ) group by stno                                                                                  \n");
		sql.append("), 0) as dep1_crd, nvl((                                  \n");
		sql.append("  select sum(crd) as total from (                                                                  \n");
		sql.append("    select b.* from (                                                                            \n");
		sql.append("        select *                                                                                 \n");
		sql.append("        from grat003 x                                                                           \n");
		sql.append("        where x.app_grad_type = '04' and x.num_of_times = (                                      \n");
		sql.append("            select a.num_of_times from grat003 a                                                 \n");
		sql.append("            where a.ayear = '" + ayear + "' and a.sms = '" + sms + "' and a.app_grad_type = '05' and x.stno = a.stno \n");
		sql.append("        ) and x.grad_reexam_status = '2'                                                         \n");
		sql.append("    ) a join grat004 b on a.ayear = b.ayear and a.sms = b.sms and a.stno = b.stno                \n");
		sql.append("    where a.stno = '" + stno + "' and b.adopt_faculty = '" + dep2 + "'                                        \n");
		sql.append("    union                                                                                        \n");
		sql.append("    select b.* from grat003 a                                                                    \n");
		sql.append("    join grat004 b on a.ayear = b.ayear and a.sms = b.sms and a.stno = b.stno                    \n");
		sql.append("    where b.ayear = '" + ayear + "' and b.sms = '" + sms + "' and a.app_grad_type = '05'                             \n");
		sql.append("    and a.stno = '" + stno + "' and b.adopt_faculty = '" + dep2 + "'                           \n");
		sql.append("  ) group by stno                                                                                  \n");
		sql.append("), 0) as dep2_crd, nvl((                                  \n");
		sql.append("  select sum(crd) as total from (                                                                  \n");
		sql.append("    select b.* from (                                                                            \n");
		sql.append("        select *                                                                                 \n");
		sql.append("        from grat003 x                                                                           \n");
		sql.append("        where x.app_grad_type = '04' and x.num_of_times = (                                      \n");
		sql.append("            select a.num_of_times from grat003 a                                                 \n");
		sql.append("            where a.ayear = '" + ayear + "' and a.sms = '" + sms + "' and a.app_grad_type = '05' and x.stno = a.stno \n");
		sql.append("        ) and x.grad_reexam_status = '2'                                                         \n");
		sql.append("    ) a join grat004 b on a.ayear = b.ayear and a.sms = b.sms and a.stno = b.stno                \n");
		sql.append("    where a.stno = '" + stno + "' and b.adopt_faculty = '90'                                        \n");
		sql.append("    union                                                                                        \n");
		sql.append("    select b.* from grat003 a                                                                    \n");
		sql.append("    join grat004 b on a.ayear = b.ayear and a.sms = b.sms and a.stno = b.stno                    \n");
		sql.append("    where b.ayear = '" + ayear + "' and b.sms = '" + sms + "' and a.app_grad_type = '05'                             \n");
		sql.append("    and a.stno = '" + stno + "' and b.adopt_faculty = '90'                                      \n");
		sql.append("  ) group by stno                                                                                  \n");
		sql.append("), 0) as common                                           \n");
		sql.append("from dual                                                 \n");

		DBResult rs = null;
		Map map = new LinkedHashMap();
		try {
			rs = dbManager.getSimpleResultSet(conn);
			rs.open();
			rs.executeQuery(sql.toString());

			if (rs.next()) {
				map.put("1", rs.getString("dep1_crd"));
				map.put("2", rs.getString("dep2_crd"));
				map.put("3", rs.getString("common"));
			} else {
				map.put("1", "0");
				map.put("2", "0");
				map.put("3", "0");
			}
		} catch (Exception e) {
			throw e;
		} finally {
			if (rs != null)
				rs.close();
		}

		return map;
	}

	/**
	 * 取得學系資料
	 *
	 * @param dbManager
	 * @param conn
	 * @return
	 * @throws Exception
	 */
	public Map getSYST003(DBManager dbManager, Connection conn) throws Exception {
		StringBuffer sql = new StringBuffer();
		sql.append("select a.faculty_code, a.faculty_name from syst003 a ");

		DBResult rs = null;
		Map map = new LinkedHashMap();
		try {
			rs = dbManager.getSimpleResultSet(conn);
			rs.open();
			rs.executeQuery(sql.toString());

			while (rs.next()) {
				map.put(rs.getString("faculty_code"), rs.getString("faculty_name"));
			}
		} catch (Exception e) {
			throw e;
		} finally {
			if (rs != null)
				rs.close();
		}

		return map;
	}

	/**
	 * 取得中心資料
	 *
	 * @param dbManager
	 * @param conn
	 * @param center_code
	 * @param rtnType
	 * @return
	 * @throws Exception
	 */
	public Map getSYST002(DBManager dbManager, Connection conn) throws Exception {
		StringBuffer sql = new StringBuffer();
		sql.append("select center_code, center_abbrname from syst002 ");

		DBResult rs = null;
		Map map = new LinkedHashMap();
		try {
			rs = dbManager.getSimpleResultSet(conn);
			rs.open();
			rs.executeQuery(sql.toString());

			while (rs.next()) {
				map.put(rs.getString("center_code"), rs.getString("center_abbrname"));
			}
		} catch (Exception e) {
			throw e;
		} finally {
			if (rs != null)
				rs.close();
		}

		return map;
	}

	/**
	 * 取得系統參數
	 *
	 * @param dbManager
	 * @param conn
	 * @param kind
	 * @return
	 * @throws Exception
	 */
	public Map getSYST001(DBManager dbManager, Connection conn, String kind) throws Exception {
		StringBuffer sql = new StringBuffer();
		sql.append("select a.code, a.code_name from syst001 a where a.kind = '" + kind + "' ");

		DBResult rs = null;
		Map map = new LinkedHashMap();
		try {
			rs = dbManager.getSimpleResultSet(conn);
			rs.open();
			rs.executeQuery(sql.toString());

			while (rs.next()) {
				map.put(rs.getString("code"), rs.getString("code_name"));
			}
		} catch (Exception e) {
			throw e;
		} finally {
			if (rs != null)
				rs.close();
		}

		return map;
	}

	public String rtnScore(String score) {

		if (score.equals("")) {
			return "&nbsp;";
		}
		if (score.indexOf(".") == -1) {
			score += ".00";
		} else if (score.substring(score.indexOf(".") + 1).length() == 1) {
			score += "0";
		}

		return score;
	}

	// 上下學期實得學分數-學系開設
	// 2008.3.29 去除暑修課程
	public int FacultyMajor(DBManager dbManager, Connection conn, String AYEAR, String SMS, String stno, String faculty_code, int lineCode) throws Exception {
		DBResult rs = null;
		int rtnValue = 0;
		try {
			GRAT004DAO GRAT004DAO = new GRAT004DAO(dbManager, conn);

			int digitOne = lineCode / 10;
			if ("90".equals(faculty_code)) {
				digitOne = 0;
			}

			StringBuffer sql = new StringBuffer();
			sql.append("select sum(crd) as total from ( ");
			sql.append("  SELECT * FROM stut010 WHERE sms in ('1', '2') ");
			sql.append("  and not exists (select a.crsno from grat027 a where a.kind = '" + digitOne + "' and a.crsno = crsno) and stno = '" + stno + "' ");

			if (!"1".equals(ht.get("STTYPE")) || !"0".equals(ht.get("TIMES"))) {
				sql.append("AND ayear || DECODE (sms, '1', '2', '2', '3', '3', '1') <= ");
				sql.append("(SELECT ayear || DECODE (sms, '1', '2', '2', '3', '3', '1') FROM grat003 WHERE stno = '" + stno + "' ");
				sql.append("AND num_of_times = (SELECT num_of_times FROM grat003 WHERE ayear = '" + AYEAR + "' AND sms = '" + SMS + "' AND stno = '" + stno + "')  ");
				sql.append("AND app_grad_type IN ('01', '02', '05') AND GRAD_REEXAM_STATUS <> '3' )   ");
				sql.append("AND ayear || DECODE (sms, '1', '2', '2', '3', '3', '1') > ");
				sql.append("DECODE ((SELECT num_of_times FROM grat003 WHERE ayear = '" + AYEAR + "' AND sms = '" + SMS + "' AND stno = '" + stno + "'), ");
				sql.append("'1', 0, (SELECT ayear || DECODE (sms, '1', '2', '2', '3', '3', '1') FROM grat003  ");
				sql.append("WHERE stno = '" + stno + "' AND num_of_times = (SELECT num_of_times - 1 FROM grat003 WHERE ayear = '" + AYEAR + "' AND sms = '" + SMS + "' AND stno = '" + stno
						+ "') AND app_grad_type IN ('01', '02', '05') AND GRAD_REEXAM_STATUS <> '3')) ");
			}

			sql.append(") a inner join cout103 b on a.crsno = b.crsno and a.ayear = b.ayear and a.sms = b.sms and a.get_manner != '2' and b.total_crs_no = '01' and b.crs_group_code = '002' ");
			sql.append(" and b.faculty_code = '" + faculty_code + "' ");

			if ("90".equals(faculty_code)) {
				sql.append(" and a.crsno in (" + getGroupCrsno(lineCode) + ") ");
			}

			if ("90".equals(faculty_code) && ("2".equals(ht.get("STTYPE")) || "3".equals(ht.get("STTYPE")) || "Y".equals(ht.get("FLAG")))) {
				sql.append("and not exists (SELECT CRSNO FROM grat004 x where x.is_summer = 'N' AND x.stno = '" + stno + "' and x.crsno = a.crsno)");
			}

			// System.out.println("FacultyMajor:"+sql);
			rs = GRAT004DAO.query(sql.toString());

			try {
				if (ht.get("stnoOrNot").equals("true")) {
					String sql2 = "SELECT '"
							+ ht.get("AYEAR")
							+ "' ayear, '"
							+ ht.get("SMS")
							+ "' as sms, stno, '"
							+ ht.get("STTYPE")
							+ "' as kind, decode('"
							+ faculty_code
							+ "', '90', '"
							+ lineCode % 10
							+ "', '0') group_code, decode('"
							+ faculty_code
							+ "', '90', '0', '10', '1', '20', '2', '30', '3', '40', '4', '50', '5', '60', '6', '') faculty_code, a.crsno, a.ayear get_ayear, a.sms get_sms, crd, '' get_manner, '' is_valid, '' upd_user_id, '' upd_date, '' upd_time, '' upd_mk, '' rowstamp from ("
							+ "SELECT * FROM stut010 WHERE sms in ('1', '2') "
							+ "and crsno not in (select a.crsno from grat027 a where a.kind = '"
							+ digitOne
							+ "') and stno = '"
							+ stno
							+ "' "
							+ ((!ht.get("STTYPE").equals("1") || !ht.get("TIMES").equals("0")) ?

							"AND ayear || DECODE (sms, '1', '2', '2', '3', '3', '1') <= " + "(SELECT ayear || DECODE (sms, '1', '2', '2', '3', '3', '1') FROM grat003 WHERE stno = '" + stno + "' "
									+ "AND num_of_times = (SELECT num_of_times FROM grat003 WHERE ayear = '" + AYEAR + "' AND sms = '" + SMS + "' AND stno = '" + stno + "')  "
									+ "AND app_grad_type IN ('01', '02', '05') AND GRAD_REEXAM_STATUS <> '3' )   " +

									"AND ayear || DECODE (sms, '1', '2', '2', '3', '3', '1') > " + "DECODE ((SELECT num_of_times FROM grat003 WHERE ayear = '" + AYEAR + "' AND sms = '" + SMS
									+ "' AND stno = '" + stno + "'), " + "'1', 0, (SELECT ayear || DECODE (sms, '1', '2', '2', '3', '3', '1') FROM grat003  " + "WHERE stno = '" + stno
									+ "' AND num_of_times = (SELECT num_of_times - 1 FROM grat003 WHERE ayear = '" + AYEAR + "' AND sms = '" + SMS + "' AND stno = '" + stno
									+ "') AND app_grad_type IN ('01', '02', '05') AND GRAD_REEXAM_STATUS <> '3')) " : "")
							+ ") a inner join cout103 b on a.crsno = b.crsno and a.ayear = b.ayear and a.sms = b.sms and a.get_manner != '2' and b.total_crs_no = '01' and b.crs_group_code = '002' "
							+ " and b.faculty_code = '"
							+ faculty_code
							+ "' "
							+ ((faculty_code.equals("90")) ? " and a.crsno in (" + getGroupCrsno(lineCode) + ") " : "")
							+ (("90".equals(faculty_code) && (ht.get("STTYPE").equals("2") || ht.get("STTYPE").equals("3") || ht.get("FLAG").equals("Y"))) ? "and a.crsno not in (SELECT CRSNO FROM grat004 a where a.is_summer = 'N' AND a.stno = '"
									+ stno + "')"
									: "");

					GRAT004DAO.execute("insert into grat033 (" + sql2 + ")");
				}
			} catch (Exception e) {
				// System.out.println(e);
			}

			if (rs.next()) {
				rtnValue = rs.getInt("TOTAL");
			}
			return rtnValue;
		} catch (Exception e) {
			throw e;
		} finally {
			if (rs != null)
				rs.close();

			dbManager.commit();
		}
	}

	/**
	 * 上下學期實得學分-學系開設
	 *
	 * @param AYEAR
	 * @param SMS
	 * @param stno
	 * @param faculty_code
	 * @param lineCode
	 * @return
	 * @throws Exception
	 */
	public String FacultyMajorStrOnly(String AYEAR, String SMS, String stno, String faculty_code, int lineCode)
			throws Exception {
		int digitOne = lineCode / 10;
		if ("90".equals(faculty_code)) {
			digitOne = 0;
		}

		StringBuffer sql = new StringBuffer();
		sql.append("select sum(crd) as total from ( ");
		sql.append("  SELECT * FROM stut010 WHERE sms in ('1', '2') ");
		sql.append("  and not exists (select a.crsno from grat027 a where a.kind = '" + digitOne + "' and a.crsno = crsno) and stno = '" + stno + "' ");

		if (!"1".equals(ht.get("STTYPE")) || !"0".equals(ht.get("TIMES"))) {
			sql.append("AND ayear || DECODE (sms, '1', '2', '2', '3', '3', '1') <= ");
			sql.append("(SELECT ayear || DECODE (sms, '1', '2', '2', '3', '3', '1') FROM grat003 WHERE stno = '" + stno + "' ");
			sql.append("AND num_of_times = (SELECT num_of_times FROM grat003 WHERE ayear = '" + AYEAR + "' AND sms = '" + SMS + "' AND stno = '" + stno
					+ "')  ");
			sql.append("AND app_grad_type IN ('01', '02', '05') AND GRAD_REEXAM_STATUS <> '3' )   ");
			sql.append("AND ayear || DECODE (sms, '1', '2', '2', '3', '3', '1') > ");
			sql.append("DECODE ((SELECT num_of_times FROM grat003 WHERE ayear = '" + AYEAR + "' AND sms = '" + SMS + "' AND stno = '" + stno + "'), ");
			sql.append("'1', 0, (SELECT ayear || DECODE (sms, '1', '2', '2', '3', '3', '1') FROM grat003  ");
			sql.append("WHERE stno = '" + stno + "' AND num_of_times = (SELECT num_of_times - 1 FROM grat003 WHERE ayear = '" + AYEAR + "' AND sms = '" + SMS
					+ "' AND stno = '" + stno + "') AND app_grad_type IN ('01', '02', '05') AND GRAD_REEXAM_STATUS <> '3')) ");
		}

		sql.append(") a inner join cout103 b on a.crsno = b.crsno and a.ayear = b.ayear and a.sms = b.sms and a.get_manner != '2' and b.total_crs_no = '01' and b.crs_group_code = '002' ");
		sql.append(" and b.faculty_code = '" + faculty_code + "' ");

		if ("90".equals(faculty_code)) {
			sql.append(" and a.crsno in (" + getGroupCrsno(lineCode) + ") ");
		}

		if ("90".equals(faculty_code) && ("2".equals(ht.get("STTYPE")) || "3".equals(ht.get("STTYPE")) || "Y".equals(ht.get("FLAG")))) {
			sql.append("and not exists (SELECT CRSNO FROM grat004 x where x.is_summer = 'N' AND x.stno = '" + stno + "' and x.crsno = a.crsno)");
		}

		return sql.toString();
	}

	public int FacultyMajor(DBManager dbManager, Connection conn, String AYEAR, String SMS, String stno, String faculty_code) throws Exception {
		return FacultyMajor(dbManager, conn, AYEAR, SMS, stno, faculty_code, 0);
	}


	//上下學期實得學分數-申請歸併
	public int FacultyCombine(DBManager dbManager, Connection conn, String AYEAR, String SMS, String stno, String faculty_code, int lineCode) throws Exception {
		DBResult	rs	=	null;
		int rtnValue = 0;
		try {
			GRAT004DAO	GRAT004DAO	=	new GRAT004DAO(dbManager, conn);

			String sql = "select sum(b.crd) as total from ( " +
						"SELECT * FROM stut010 WHERE stno = '" + stno + "' " +
	   					"AND ayear || DECODE (sms, '1', '2', '2', '3', '3', '1') <= " +
	          				"nvl( (SELECT ayear || DECODE (sms, '1', '2', '2', '3', '3', '1') FROM grat003 WHERE stno = '" + stno + "' " +
	              				"AND num_of_times = (SELECT num_of_times FROM grat003 WHERE ayear = '" + AYEAR + "' AND sms = '" + SMS + "' AND stno = '" + stno + "')  " +
				  		"AND app_grad_type IN ('01', '02', '05') AND GRAD_REEXAM_STATUS <> '3'), 9999) " +
	   					"AND ayear || DECODE (sms, '1', '2', '2', '3', '3', '1') > " +
	          				"nvl(DECODE ((SELECT num_of_times FROM grat003 WHERE ayear = '" + AYEAR + "' AND sms = '" + SMS + "' AND stno = '" + stno + "'), " +
	                  			"'1', 0, (SELECT ayear || DECODE (sms, '1', '2', '2', '3', '3', '1') FROM grat003  " +
					  	"WHERE stno = '" + stno + "' AND num_of_times = (SELECT num_of_times - 1 FROM grat003 WHERE ayear = '" + AYEAR + "' AND sms = '" + SMS + "' AND stno = '" + stno + "') AND app_grad_type IN ('01', '02', '05') AND GRAD_REEXAM_STATUS <> '3')), 0) " +
						") a inner join grat004 b on a.crsno = b.crsno and a.get_manner != '2' and b.adopt_faculty = '" + faculty_code + "' " +
						((faculty_code.equals("90"))?" and a.crsno in (" + getGroupCrsno(lineCode) + ") ":"") +
						"and a.stno = b.stno and b.is_summer = 'N'";

			String sql2 = "SELECT '" + ht.get("AYEAR") + "' ayear, '" + ht.get("SMS") + "' as sms, a.stno, '" + ht.get("STTYPE") + "' as kind, decode('" + faculty_code + "', '90', '" + lineCode%10 + "', '0') group_code, decode('" + faculty_code + "', '90', '0', '10', '1', '20', '2', '30', '3', '40', '4', '50', '5', '60', '6', '') faculty_code, a.crsno, a.ayear get_ayear, a.sms get_sms, a.crd, '' get_manner, '' is_valid, '' upd_user_id, '' upd_date, '' upd_time, '' upd_mk, '' rowstamp from ( " +
						"SELECT * FROM stut010 WHERE stno = '" + stno + "' " +
						"AND ayear || DECODE (sms, '1', '2', '2', '3', '3', '1') <= " +
						"nvl( (SELECT ayear || DECODE (sms, '1', '2', '2', '3', '3', '1') FROM grat003 WHERE stno = '" + stno + "' " +
						"AND num_of_times = (SELECT num_of_times FROM grat003 WHERE ayear = '" + AYEAR + "' AND sms = '" + SMS + "' AND stno = '" + stno + "')  " +
						"AND app_grad_type IN ('01', '02', '05') AND GRAD_REEXAM_STATUS <> '3'), 9999) " +
						"AND ayear || DECODE (sms, '1', '2', '2', '3', '3', '1') > " +
						"nvl( DECODE ((SELECT num_of_times FROM grat003 WHERE ayear = '" + AYEAR + "' AND sms = '" + SMS + "' AND stno = '" + stno + "'), " +
						"'1', 0, (SELECT ayear || DECODE (sms, '1', '2', '2', '3', '3', '1') FROM grat003  " +
						"WHERE stno = '" + stno + "' AND num_of_times = (SELECT num_of_times - 1 FROM grat003 WHERE ayear = '" + AYEAR + "' AND sms = '" + SMS + "' AND stno = '" + stno + "') AND app_grad_type IN ('01', '02', '05') AND GRAD_REEXAM_STATUS <> '3')), 0) " +
						") a inner join grat004 b on a.crsno = b.crsno and a.get_manner != '2' and b.adopt_faculty = '" + faculty_code + "' " +
						((faculty_code.equals("90"))?" and a.crsno in (" + getGroupCrsno(lineCode) + ") ":"") +
						"and a.stno = b.stno and b.is_summer = 'N'";


			rs	=	GRAT004DAO.query(sql);

			try {
				if(ht.get("stnoOrNot").equals("true")) {
					GRAT004DAO.execute("insert into grat033 (" + sql2 + ")");
				}
			} catch(Exception e) {
				//System.out.println(e);
			}

			System.out.println("FacultyCombine:"+sql);
			if (rs.next()) {
				rtnValue = rs.getInt("TOTAL");
			}
			return rtnValue;
		} catch(Exception e) {
			throw e;
		} finally {
			if (rs != null)
				rs.close();
		}
	}

	/**
	 * 上下學期實得學分數-申請歸併
	 *
	 * @param AYEAR
	 * @param SMS
	 * @param stno
	 * @param faculty_code
	 * @param lineCode
	 * @return
	 * @throws Exception
	 */
	public String FacultyCombineStrOnly(String AYEAR, String SMS, String stno, String faculty_code, int lineCode) throws Exception {
		StringBuffer sql = new StringBuffer();
		sql.append("select sum(b.crd) as total from ( ");
		sql.append("SELECT * FROM stut010 WHERE stno = '" + stno + "' ");
		sql.append("AND ayear || DECODE (sms, '1', '2', '2', '3', '3', '1') <= ");
		sql.append("nvl( (SELECT ayear || DECODE (sms, '1', '2', '2', '3', '3', '1') FROM grat003 WHERE stno = '" + stno + "' ");
		sql.append("AND num_of_times = (SELECT num_of_times FROM grat003 WHERE ayear = '" + AYEAR + "' AND sms = '" + SMS + "' AND stno = '" + stno + "')  ");
		sql.append("AND app_grad_type IN ('01', '02', '05') AND GRAD_REEXAM_STATUS <> '3'), 9999) ");
		sql.append("AND ayear || DECODE (sms, '1', '2', '2', '3', '3', '1') > ");
		sql.append("nvl(DECODE ((SELECT num_of_times FROM grat003 WHERE ayear = '" + AYEAR + "' AND sms = '" + SMS + "' AND stno = '" + stno + "'), ");
		sql.append("'1', 0, (SELECT ayear || DECODE (sms, '1', '2', '2', '3', '3', '1') FROM grat003  ");
		sql.append("WHERE stno = '" + stno + "' AND num_of_times = (SELECT num_of_times - 1 FROM grat003 WHERE ayear = '" + AYEAR + "' AND sms = '" + SMS
				+ "' AND stno = '" + stno + "') AND app_grad_type IN ('01', '02', '05') AND GRAD_REEXAM_STATUS <> '3')), 0) ");
		sql.append(") a inner join grat004 b on a.crsno = b.crsno and a.get_manner != '2' and b.adopt_faculty = '" + faculty_code + "' ");
		sql.append(((faculty_code.equals("90")) ? " and a.crsno in (" + getGroupCrsno(lineCode) + ") " : ""));
		sql.append("and a.stno = b.stno and b.is_summer = 'N'");

		return sql.toString();
	}

	public int FacultyCombine(DBManager dbManager, Connection conn, String AYEAR, String SMS, String stno, String faculty_code) throws Exception {
		return FacultyCombine(dbManager, conn, AYEAR, SMS, stno, faculty_code, 0);
	}

	// 該學期選修學分數-學系開設
	public int ThisSMSFacultyMajor(DBManager dbManager, Connection conn, String AYEAR, String SMS, String stno, String faculty_code, int lineCode) throws Exception {
		DBResult	rs	=	null;
		int rtnValue = 0;
		try {
			GRAT004DAO	GRAT004DAO	=	new GRAT004DAO(dbManager, conn);

			String sql = "SELECT SUM (c.crd) AS total " +
	  					"FROM cout103 a INNER JOIN regt007 b ON a.ayear = b.ayear " +
	                    "               AND a.sms = b.sms " +
	                    "               AND a.crsno = b.crsno " +
	                    "               AND a.total_crs_no = '01' " +
	                    "               AND a.crs_group_code = '002' " +
	                    ((faculty_code.equals("90"))?" and a.crsno in (" + getGroupCrsno(lineCode) + ") ":"") +
	                    ((ht.get("STTYPE").equals("2") || ht.get("STTYPE").equals("3") || ht.get("FLAG").equals("Y"))?"				and a.crsno not in (SELECT CRSNO FROM grat004 a where a.is_summer = 'N' AND a.stno = '" + stno + "')":"") +
	                    "               AND a.faculty_code = '" + faculty_code + "' " +
	                    "               AND a.ayear = '" + AYEAR + "' " +
	                    "               AND a.sms = '" + SMS + "' " +
	                    "               AND b.stno = '" + stno + "' " +
	       				"INNER JOIN cout002 c ON a.crsno = c.crsno ";

			String sql2 = "SELECT '" + ht.get("AYEAR") + "' ayear, '" + ht.get("SMS") + "' as sms, stno, '" + ht.get("STTYPE") + "' as kind, decode('" + faculty_code + "', '90', '" + lineCode%10 + "', '0') group_code, decode('" + faculty_code + "', '90', '0', '10', '1', '20', '2', '30', '3', '40', '4', '50', '5', '60', '6', '') faculty_code, a.crsno, d.ayear get_ayear, d.sms get_sms, crd, '' get_manner, '' is_valid, '' upd_user_id, '' upd_date, '' upd_time, '' upd_mk, '' rowstamp " +
						"FROM cout103 a INNER JOIN regt007 b ON a.ayear = b.ayear " +
			            "               AND a.sms = b.sms " +
			            "               AND a.crsno = b.crsno " +
			            "               AND a.total_crs_no = '01' " +
			            "               AND a.crs_group_code = '002' " +
			            ((faculty_code.equals("90"))?" and a.crsno in (" + getGroupCrsno(lineCode) + ") ":"") +
			            ((ht.get("STTYPE").equals("2") || ht.get("STTYPE").equals("3") || ht.get("FLAG").equals("Y"))?"				and a.crsno not in (SELECT CRSNO FROM grat004 a where a.is_summer = 'N' AND a.stno = '" + stno + "')":"") +
			            "               AND a.faculty_code = '" + faculty_code + "' " +
			            "               AND a.ayear = '" + AYEAR + "' " +
			            "               AND a.sms = '" + SMS + "' " +
			            "               AND b.stno = '" + stno + "' " +
			            "INNER JOIN cout002 c ON a.crsno = c.crsno " +
			            "INNER JOIN stut010 d on b.stno = d.stno and b.crsno = d.crsno";


			//System.out.println("ThisSMSFacultyMajor:"+sql);
			//System.out.println("ThisSMSFacultyMajor:"+sql2);
			rs	=	GRAT004DAO.query(sql);

			try {
				if(ht.get("stnoOrNot").equals("true")) {
					GRAT004DAO.execute("insert into grat033 (" + sql2 + ")");
				}
			} catch(Exception e) {
				//System.out.println(e);
			}

			if (rs.next()) {
				rtnValue = rs.getInt("TOTAL");
			}
			return rtnValue;
		} catch(Exception e) {
			throw e;
		} finally {
			if (rs != null)
				rs.close();
		}
	}

	/**
	 * 該學期選修學分數-學系開設
	 *
	 * @param AYEAR
	 * @param SMS
	 * @param stno
	 * @param faculty_code
	 * @param lineCode
	 * @return
	 * @throws Exception
	 */
	public String ThisSMSFacultyMajorStrOnly(String AYEAR, String SMS, String stno, String faculty_code, int lineCode) throws Exception {
		StringBuffer sql = new StringBuffer();
		sql.append("SELECT SUM (c.crd) AS total ");
		sql.append("FROM cout103 a INNER JOIN regt007 b ON a.ayear = b.ayear ");
		sql.append("               AND a.sms = b.sms ");
		sql.append("               AND a.crsno = b.crsno ");
		sql.append("               AND a.total_crs_no = '01' ");
		sql.append("               AND a.crs_group_code = '002' ");
		sql.append(((faculty_code.equals("90")) ? " and a.crsno in (" + getGroupCrsno(lineCode) + ") " : ""));
		sql.append(((ht.get("STTYPE").equals("2") || ht.get("STTYPE").equals("3") || ht.get("FLAG").equals("Y")) ? "				and a.crsno not in (SELECT CRSNO FROM grat004 a where a.is_summer = 'N' AND a.stno = '"
						+ stno + "')"
						: ""));
		sql.append("               AND a.faculty_code = '" + faculty_code + "' ");
		sql.append("               AND a.ayear = '" + AYEAR + "' ");
		sql.append("               AND a.sms = '" + SMS + "' ");
		sql.append("               AND b.stno = '" + stno + "' ");
		sql.append("INNER JOIN cout002 c ON a.crsno = c.crsno ");

		return sql.toString();
	}

	public int ThisSMSFacultyMajor(DBManager dbManager, Connection conn, String AYEAR, String SMS, String stno, String faculty_code) throws Exception {
		return ThisSMSFacultyMajor(dbManager, conn, AYEAR, SMS, stno, faculty_code, 0);
	}

	// 該學期選修學分數-申請歸併
	public int ThisSMSFacultyCombine(DBManager dbManager, Connection conn, String AYEAR, String SMS, String stno, String faculty_code, int lineCode) throws Exception {
		DBResult	rs	=	null;
		int rtnValue = 0;
		try {
			GRAT004DAO	GRAT004DAO	=	new GRAT004DAO(dbManager, conn);

			String sql = "SELECT SUM (a.crd) AS total " +
	  				 "FROM grat004 a INNER JOIN cout002 b ON a.crsno = b.crsno " +
	                 "                  AND a.is_summer = 'N' " +
	                 "                  AND (a.ayear || a.sms) = '" + AYEAR + SMS + "' " +
	                 "                  AND a.stno = '" + stno + "' " +
	                 ((faculty_code.equals("90"))?" and a.crsno in (" + getGroupCrsno(lineCode) + ") ":"") +
	                 "                  AND adopt_faculty = '" + faculty_code + "'" +
	                 " inner join regt007 c on a.ayear = c.ayear and a.sms = c.sms and a.stno = c.stno and a.crsno = c.crsno ";

			String sql2 = "SELECT '" + ht.get("AYEAR") + "' ayear, '" + ht.get("SMS") + "' as sms, stno, '" + ht.get("STTYPE") + "' as kind, decode('" + faculty_code + "', '90', '" + lineCode%10 + "', '0') group_code, decode('" + faculty_code + "', '90', '0', '10', '1', '20', '2', '30', '3', '40', '4', '50', '5', '60', '6', '') faculty_code, a.crsno, d.ayear get_ayear, d.sms get_sms, a.crd, '' get_manner, '' is_valid, '' upd_user_id, '' upd_date, '' upd_time, '' upd_mk, '' rowstamp	 " +
						"FROM grat004 a INNER JOIN cout002 b ON a.crsno = b.crsno " +
			            "                  AND a.is_summer = 'N' " +
			            "                  AND (a.ayear || a.sms) = '" + AYEAR + SMS + "' " +
			            "                  AND a.stno = '" + stno + "' " +
			            ((faculty_code.equals("90"))?" and a.crsno in (" + getGroupCrsno(lineCode) + ") ":"") +
			            "                  AND adopt_faculty = '" + faculty_code + "'" +
			            " inner join regt007 c on a.ayear = c.ayear and a.sms = c.sms and a.stno = c.stno and a.crsno = c.crsno " +
			            " inner join stut010 d on c.stno = d.stno and c.crsno = d.crsno ";

			//System.out.println("ThisSMSFacultyCombine:"+sql);
			rs	=	GRAT004DAO.query(sql);


			try {
				if(ht.get("stnoOrNot").equals("true")) {
					GRAT004DAO.execute("insert into grat033 (" + sql2 + ")");
				}
			} catch(Exception e) {
				//System.out.println(e);
			}

			if (rs.next()) {
				rtnValue = rs.getInt("TOTAL");
			}
			return rtnValue;
		} catch(Exception e) {
			throw e;
		} finally {
			if (rs != null)
				rs.close();
		}
	}

	/**
	 * 該學期選修學分數-申請歸併
	 *
	 * @param AYEAR
	 * @param SMS
	 * @param stno
	 * @param faculty_code
	 * @param lineCode
	 * @return
	 * @throws Exception
	 */
	public String ThisSMSFacultyCombineStrOnly(String AYEAR, String SMS, String stno, String faculty_code, int lineCode) throws Exception {
		StringBuffer sql = new StringBuffer();
		sql.append("SELECT SUM (a.crd) AS total ");
		sql.append("FROM grat004 a INNER JOIN cout002 b ON a.crsno = b.crsno ");
		sql.append("                  AND a.is_summer = 'N' ");
		sql.append("                  AND (a.ayear || a.sms) = '" + AYEAR + SMS + "' ");
		sql.append("                  AND a.stno = '" + stno + "' ");
		sql.append(((faculty_code.equals("90")) ? " and a.crsno in (" + getGroupCrsno(lineCode) + ") " : ""));
		sql.append("                  AND adopt_faculty = '" + faculty_code + "'");
		sql.append(" inner join regt007 c on a.ayear = c.ayear and a.sms = c.sms and a.stno = c.stno and a.crsno = c.crsno ");

		return sql.toString();
	}

	public int ThisSMSFacultyCombine(DBManager dbManager, Connection conn, String AYEAR, String SMS, String stno, String faculty_code) throws Exception {
		return ThisSMSFacultyCombine(dbManager, conn, AYEAR, SMS, stno, faculty_code, 0);
	}

	// 暑期課程實得學分數-學系開設
	public int SummerAllFacultyMajor(DBManager dbManager, Connection conn, String AYEAR, String SMS, String stno, String faculty_code, int lineCode) throws Exception {
		DBResult	rs	=	null;
		int rtnValue = 0;
		try {
			GRAT004DAO	GRAT004DAO	=	new GRAT004DAO(dbManager, conn);

			String sql = "SELECT sum(a.crd) as total " +
						"FROM stut010 a inner join cout103 b on a.ayear = b.ayear and a.sms = b.sms and a.crsno = b.crsno and b.total_crs_no = '01' and b.crs_group_code = '002'  " +
						"WHERE a.stno = '" + stno + "' and a.sms = '3' " +
						((faculty_code.equals("90"))?" and a.crsno in (" + getGroupCrsno(lineCode) + ") ":"") +
						((!ht.get("STTYPE").equals("1") || !ht.get("TIMES").equals("0"))?
						"AND a.ayear || DECODE (a.sms, '1', '2', '2', '3', '3', '1') <= " +
						"(SELECT ayear || DECODE (sms, '1', '2', '2', '3', '3', '1') " +
						"FROM grat003 " +
						"WHERE stno = '" + stno + "' " +
						"AND num_of_times = " +
                     	"(SELECT num_of_times " +
                     	"FROM grat003 " +
                       	"WHERE ayear = '" + AYEAR + "' AND sms = '" + SMS + "' " +
                        "    AND stno = '" + stno + "') " +
						"AND app_grad_type IN ('01', '02', '05') AND GRAD_REEXAM_STATUS <> '3') " +

						"AND a.ayear || DECODE (a.sms, '1', '2', '2', '3', '3', '1') > " +
						"DECODE ((SELECT num_of_times " +
                     	"FROM grat003 " +
                    	"WHERE ayear = '" + AYEAR + "' AND sms = '" + SMS + "' AND stno = '" + stno + "'), " +
                  		"'1', 0, " +
                  		"(SELECT ayear || DECODE (sms, '1', '2', '2', '3', '3', '1') " +
                  		"FROM grat003 " +
                    	"WHERE stno = '" + stno + "' " +
                      	"AND num_of_times = " +
                        "    (SELECT num_of_times - 1 " +
                        "       FROM grat003 " +
                        "      WHERE ayear = '" + AYEAR + "' " +
                        "        AND sms = '" + SMS + "' " +
                        "        AND stno = '" + stno + "') " +
                      	"AND app_grad_type IN ('01', '02', '05') AND GRAD_REEXAM_STATUS <> '3') )":"") +

                        ((ht.get("STTYPE").equals("2") || ht.get("STTYPE").equals("3") || ht.get("FLAG").equals("Y"))?"and a.crsno not in (SELECT CRSNO FROM grat004 a where a.is_summer = 'Y' AND a.stno = '" + stno + "' and adopt_faculty is not null)":"") +
                        "and a.crsno not in (select crsno from cout001 c where c.new_rework = '3' and c.reqoption_type = '1' and c.crd >= 3 and c.ayear = a.ayear and c.sms = a.sms) " +
                        ((!faculty_code.equals("90"))?"   AND A.CRSNO NOT IN (SELECT A.CRSNO FROM STUT010 A JOIN COUT103 B ON A.AYEAR = B.AYEAR AND A.SMS = B.SMS AND A.CRSNO = B.CRSNO WHERE A.STNO = '" + stno + "' AND B.TOTAL_CRS_NO = '01' AND B.CRS_GROUP_CODE = '002' AND B.FACULTY_CODE = '90' AND A.SMS = '3' )":"") +
                        "and b.faculty_code = '" + faculty_code + "'";

			String sql2 = "SELECT '" + ht.get("AYEAR") + "' ayear, '" + ht.get("SMS") + "' as sms, stno, '" + ht.get("STTYPE") + "' as kind, decode('" + faculty_code + "', '90', '" + lineCode%10 + "', '0') group_code, decode('" + faculty_code + "', '90', '0', '10', '1', '20', '2', '30', '3', '40', '4', '50', '5', '60', '6', '') faculty_code, a.crsno, a.ayear get_ayear, a.sms get_sms, a.crd, '' get_manner, '' is_valid, '' upd_user_id, '' upd_date, '' upd_time, '' upd_mk, '' rowstamp " +
						"FROM stut010 a inner join cout103 b on a.ayear = b.ayear and a.sms = b.sms and a.crsno = b.crsno and b.total_crs_no = '01' and b.crs_group_code = '002'  " +
						"WHERE a.stno = '" + stno + "' and a.sms = '3' " +
						((faculty_code.equals("90"))?" and a.crsno in (" + getGroupCrsno(lineCode) + ") ":"") +
						((!ht.get("STTYPE").equals("1") || !ht.get("TIMES").equals("0"))?
						"AND a.ayear || DECODE (a.sms, '1', '2', '2', '3', '3', '1') <= " +
						"(SELECT ayear || DECODE (sms, '1', '2', '2', '3', '3', '1') " +
						"FROM grat003 " +
						"WHERE stno = '" + stno + "' " +
						"AND num_of_times = " +
			         	"(SELECT num_of_times " +
			         	"FROM grat003 " +
			           	"WHERE ayear = '" + AYEAR + "' AND sms = '" + SMS + "' " +
			            "    AND stno = '" + stno + "') " +
						"AND app_grad_type IN ('01', '02', '05') AND GRAD_REEXAM_STATUS <> '3') " +

						"AND a.ayear || DECODE (a.sms, '1', '2', '2', '3', '3', '1') > " +
						"DECODE ((SELECT num_of_times " +
			         	"FROM grat003 " +
			        	"WHERE ayear = '" + AYEAR + "' AND sms = '" + SMS + "' AND stno = '" + stno + "'), " +
			      		"'1', 0, " +
			      		"(SELECT ayear || DECODE (sms, '1', '2', '2', '3', '3', '1') " +
			      		"FROM grat003 " +
			        	"WHERE stno = '" + stno + "' " +
			          	"AND num_of_times = " +
			            "    (SELECT num_of_times - 1 " +
			            "       FROM grat003 " +
			            "      WHERE ayear = '" + AYEAR + "' " +
			            "        AND sms = '" + SMS + "' " +
			            "        AND stno = '" + stno + "') " +
			          	"AND app_grad_type IN ('01', '02', '05') AND GRAD_REEXAM_STATUS <> '3') )":"") +

			            ((ht.get("STTYPE").equals("2") || ht.get("STTYPE").equals("3") || ht.get("FLAG").equals("Y"))?"and a.crsno not in (SELECT CRSNO FROM grat004 a where a.is_summer = 'Y' AND a.stno = '" + stno + "' and adopt_faculty is not null)":"") +
			            "and a.crsno not in (select crsno from cout001 c where c.new_rework = '3' and c.reqoption_type = '1' and c.crd >= 3 and c.ayear = a.ayear and c.sms = a.sms) " +
			            ((!faculty_code.equals("90"))?"   AND A.CRSNO NOT IN (SELECT A.CRSNO FROM STUT010 A JOIN COUT103 B ON A.AYEAR = B.AYEAR AND A.SMS = B.SMS AND A.CRSNO = B.CRSNO WHERE A.STNO = '" + stno + "' AND B.TOTAL_CRS_NO = '01' AND B.CRS_GROUP_CODE = '002' AND B.FACULTY_CODE = '90' AND A.SMS = '3' )":"") +
			            "and b.faculty_code = '" + faculty_code + "'";

			//System.out.println("SummerAllFacultyMajor:"+sql);
			rs	=	GRAT004DAO.query(sql);

			try {
				if(ht.get("stnoOrNot").equals("true")) {
					GRAT004DAO.execute("insert into grat033 (" + sql2 + ")");
				}
			} catch(Exception e) {
				//System.out.println(e);
			}

			if (rs.next()) {
				rtnValue = rs.getInt("TOTAL");
			}
			return rtnValue;
		} catch(Exception e) {
			throw e;
		} finally {
			if (rs != null)
				rs.close();
		}
	}

	/**
	 * 暑期課程實得學分數-學系開設
	 *
	 * @param AYEAR
	 * @param SMS
	 * @param stno
	 * @param faculty_code
	 * @param lineCode
	 * @return
	 * @throws Exception
	 */
	public String SummerAllFacultyMajorStrOnly(String AYEAR, String SMS, String stno, String faculty_code, int lineCode) throws Exception {
		// StringBuffer sql = new StringBuffer();
		String sql = "SELECT sum(a.crd) as total "
				+ "FROM stut010 a inner join cout103 b on a.ayear = b.ayear and a.sms = b.sms and a.crsno = b.crsno and b.total_crs_no = '01' and b.crs_group_code = '002'  "
				+ "WHERE a.stno = '"
				+ stno
				+ "' and a.sms = '3' "
				+ ((faculty_code.equals("90")) ? " and a.crsno in (" + getGroupCrsno(lineCode) + ") " : "")
				+ ((!ht.get("STTYPE").equals("1") || !ht.get("TIMES").equals("0")) ? "AND a.ayear || DECODE (a.sms, '1', '2', '2', '3', '3', '1') <= "
						+ "(SELECT ayear || DECODE (sms, '1', '2', '2', '3', '3', '1') " + "FROM grat003 " + "WHERE stno = '"
						+ stno
						+ "' "
						+ "AND num_of_times = "
						+ "(SELECT num_of_times "
						+ "FROM grat003 "
						+ "WHERE ayear = '"
						+ AYEAR
						+ "' AND sms = '"
						+ SMS
						+ "' "
						+ "    AND stno = '"
						+ stno
						+ "') "
						+ "AND app_grad_type IN ('01', '02', '05') AND GRAD_REEXAM_STATUS <> '3') "
						+

						"AND a.ayear || DECODE (a.sms, '1', '2', '2', '3', '3', '1') > "
						+ "DECODE ((SELECT num_of_times "
						+ "FROM grat003 "
						+ "WHERE ayear = '"
						+ AYEAR
						+ "' AND sms = '"
						+ SMS
						+ "' AND stno = '"
						+ stno
						+ "'), "
						+ "'1', 0, "
						+ "(SELECT ayear || DECODE (sms, '1', '2', '2', '3', '3', '1') "
						+ "FROM grat003 "
						+ "WHERE stno = '"
						+ stno
						+ "' "
						+ "AND num_of_times = "
						+ "    (SELECT num_of_times - 1 "
						+ "       FROM grat003 "
						+ "      WHERE ayear = '"
						+ AYEAR
						+ "' "
						+ "        AND sms = '"
						+ SMS
						+ "' "
						+ "        AND stno = '"
						+ stno
						+ "') "
						+ "AND app_grad_type IN ('01', '02', '05') AND GRAD_REEXAM_STATUS <> '3') )" : "")
				+

				((ht.get("STTYPE").equals("2") || ht.get("STTYPE").equals("3") || ht.get("FLAG").equals("Y")) ? "and a.crsno not in (SELECT CRSNO FROM grat004 a where a.is_summer = 'Y' AND a.stno = '"
						+ stno + "' and adopt_faculty is not null)"
						: "")
				+ "and a.crsno not in (select crsno from cout001 c where c.new_rework = '3' and c.reqoption_type = '1' and c.crd >= 3 and c.ayear = a.ayear and c.sms = a.sms) "
				+ ((!faculty_code.equals("90")) ? "   AND A.CRSNO NOT IN (SELECT A.CRSNO FROM STUT010 A JOIN COUT103 B ON A.AYEAR = B.AYEAR AND A.SMS = B.SMS AND A.CRSNO = B.CRSNO WHERE A.STNO = '"
						+ stno + "' AND B.TOTAL_CRS_NO = '01' AND B.CRS_GROUP_CODE = '002' AND B.FACULTY_CODE = '90' AND A.SMS = '3' )"
						: "") + "and b.faculty_code = '" + faculty_code + "'";

		return sql;
	}

	public int SummerAllFacultyMajor(DBManager dbManager, Connection conn, String AYEAR, String SMS, String stno, String faculty_code) throws Exception {
		return SummerAllFacultyMajor(dbManager, conn, AYEAR, SMS, stno, faculty_code, 0);
	}


	//暑期課程採計學分數-學系開設
	public int SummerFacultyMajor(DBManager dbManager, Connection conn, String AYEAR, String SMS, String stno, String faculty_code, int lineCode) throws Exception {
		DBResult	rs	=	null;
		int rtnValue = 0;
		try {
			GRAT004DAO	GRAT004DAO	=	new GRAT004DAO(dbManager, conn);

			String sql = "select sum(crd) as total from grat004 where 1 = 1 " +
					((!ht.get("STTYPE").equals("1") || !ht.get("TIMES").equals("0"))?
					"and ayear || sms = (select min(ayear || sms) from grat003 where stno = '" + stno + "' and num_of_times in (select num_of_times from grat003 where ayear = '" + AYEAR + "' and sms = '" + SMS + "' and stno = '" + stno + "')) ":"") +
					"and stno = '" + stno + "' and is_summer = 'Y' " +
					"and faculty_code = '" + faculty_code + "' " + ((faculty_code.equals("90"))?" and crsno in (" + getGroupCrsno(lineCode) + ") ":"");

			String sql2 = "SELECT '" + ht.get("AYEAR") + "' ayear, '" + ht.get("SMS") + "' as sms, grat004.stno, '" + ht.get("STTYPE") + "' as kind, decode('" + faculty_code + "', '90', '" + lineCode%10 + "', '0') group_code, decode('" + faculty_code + "', '90', '0', '10', '1', '20', '2', '30', '3', '40', '4', '50', '5', '60', '6', '') faculty_code, grat004.crsno, stut010.ayear get_ayear, stut010.sms get_sms, grat004.crd, '' get_manner, '' is_valid, '' upd_user_id, '' upd_date, '' upd_time, '' upd_mk, '' rowstamp from grat004 inner join stut010 on grat004.stno = stut010.stno and grat004.crsno = stut010.crsno where 1 = 1 " +
					((!ht.get("STTYPE").equals("1") || !ht.get("TIMES").equals("0"))?
					"and ayear || sms = (select min(ayear || sms) from grat003 where stno = '" + stno + "' and num_of_times in (select num_of_times from grat003 where ayear = '" + AYEAR + "' and sms = '" + SMS + "' and stno = '" + stno + "')) ":"") +
					"and grat004.stno = '" + stno + "' and is_summer = 'Y' " +
					"and faculty_code = '" + faculty_code + "' " + ((faculty_code.equals("90"))?" and crsno in (" + getGroupCrsno(lineCode) + ") ":"");


			//System.out.println("SummerFacultyMajor:"+sql2);
			rs	=	GRAT004DAO.query(sql);

			try {
				if(ht.get("stnoOrNot").equals("true")) {
					GRAT004DAO.execute("insert into grat033 (" + sql2 + ")");
				}
			} catch(Exception e) {
				//System.out.println(e);
			}

			if (rs.next()) {
				rtnValue = rs.getInt("TOTAL");
			}
			return rtnValue;
		} catch(Exception e) {
			throw e;
		} finally {
			if (rs != null)
				rs.close();
		}
	}

	/**
	 * 暑期課程採計學分數-學系開設
	 *
	 * @param AYEAR
	 * @param SMS
	 * @param stno
	 * @param faculty_code
	 * @param lineCode
	 * @return
	 * @throws Exception
	 */
	public String SummerFacultyMajorStrOnly(String AYEAR, String SMS, String stno, String faculty_code, int lineCode) throws Exception {
		String sql = "select sum(crd) as total from grat004 where 1 = 1 "
				+ ((!ht.get("STTYPE").equals("1") || !ht.get("TIMES").equals("0")) ? "and ayear || sms = (select min(ayear || sms) from grat003 where stno = '"
						+ stno + "' and num_of_times in (select num_of_times from grat003 where ayear = '" + AYEAR + "' and sms = '" + SMS + "' and stno = '"
						+ stno + "')) " : "") + "and stno = '" + stno + "' and is_summer = 'Y' " + "and faculty_code = '" + faculty_code + "' "
				+ ((faculty_code.equals("90")) ? " and crsno in (" + getGroupCrsno(lineCode) + ") " : "");

		return sql;
	}

	public int SummerFacultyMajor(DBManager dbManager, Connection conn, String AYEAR, String SMS, String stno, String faculty_code) throws Exception {
		return SummerFacultyMajor(dbManager, conn, AYEAR, SMS, stno, faculty_code, 0);
	}

	//暑期課程實得學分數-申請歸併 & 暑期課程採計學分數-申請歸併
	public int SummerFacultyCombine(DBManager dbManager, Connection conn, String AYEAR, String SMS, String stno, String faculty_code, int lineCode) throws Exception {
		DBResult	rs	=	null;
		int rtnValue = 0;
		try {
			GRAT004DAO	GRAT004DAO	=	new GRAT004DAO(dbManager, conn);

			String sql = "select sum(crd) as total from grat004 where 1 = 1 " +
					((!ht.get("STTYPE").equals("1") || !ht.get("TIMES").equals("0"))?
					"and ayear || sms = (select min(ayear || sms) from grat003 where stno = '" + stno + "' and num_of_times in (select num_of_times from grat003 where ayear = '" + AYEAR + "' and sms = '" + SMS + "' and stno = '" + stno + "')) ":"") +
					"and stno = '" + stno + "' and is_summer = 'Y' and " +
					"adopt_faculty = '" + faculty_code + "' " + ((faculty_code.equals("90"))?" and crsno in (" + getGroupCrsno(lineCode) + ") ":"");

			String sql2 = "SELECT '" + ht.get("AYEAR") + "' ayear, '" + ht.get("SMS") + "' as sms, grat004.stno, '" + ht.get("STTYPE") + "' as kind, decode('" + faculty_code + "', '90', '" + lineCode%10 + "', '0') group_code, decode('" + faculty_code + "', '90', '0', '10', '1', '20', '2', '30', '3', '40', '4', '50', '5', '60', '6', '') faculty_code, grat004.crsno, stut010.ayear get_ayear, stut010.sms get_sms, grat004.crd, '' get_manner, '' is_valid, '' upd_user_id, '' upd_date, '' upd_time, '' upd_mk, '' rowstamp  from grat004 inner join stut010 on grat004.stno = stut010.stno and grat004.crsno = stut010.crsno where 1 = 1 " +
					((!ht.get("STTYPE").equals("1") || !ht.get("TIMES").equals("0"))?
					"and ayear || sms = (select min(ayear || sms) from grat003 where stno = '" + stno + "' and num_of_times in (select num_of_times from grat003 where ayear = '" + AYEAR + "' and sms = '" + SMS + "' and stno = '" + stno + "')) ":"") +
					"and grat004.stno = '" + stno + "' and is_summer = 'Y' and " +
					"adopt_faculty = '" + faculty_code + "' " + ((faculty_code.equals("90"))?" and crsno in (" + getGroupCrsno(lineCode) + ") ":"");



			//System.out.println("SummerFacultyCombine:"+sql2);
			rs	=	GRAT004DAO.query(sql);
			try {
				if(ht.get("stnoOrNot").equals("true")) {
					GRAT004DAO.execute("insert into grat033 (" + sql2 + ")");
				}
			} catch(Exception e) {
				//System.out.println(e);
			}

			if (rs.next()) {
				rtnValue = rs.getInt("TOTAL");
			}
			return rtnValue;
		} catch(Exception e) {
			throw e;
		} finally {
			if (rs != null)
				rs.close();
		}
	}

	/**
	 * 暑期課程實得學分數-申請歸併 & 暑期課程採計學分數-申請歸併
	 *
	 * @param AYEAR
	 * @param SMS
	 * @param stno
	 * @param faculty_code
	 * @param lineCode
	 * @return
	 * @throws Exception
	 */
	public String SummerFacultyCombineStrOnly(String AYEAR, String SMS, String stno, String faculty_code, int lineCode) throws Exception {
		String sql = "select sum(crd) as total from grat004 where 1 = 1 "
				+ ((!ht.get("STTYPE").equals("1") || !ht.get("TIMES").equals("0")) ? "and ayear || sms = (select min(ayear || sms) from grat003 where stno = '"
						+ stno + "' and num_of_times in (select num_of_times from grat003 where ayear = '" + AYEAR + "' and sms = '" + SMS + "' and stno = '"
						+ stno + "')) " : "") + "and stno = '" + stno + "' and is_summer = 'Y' and " + "adopt_faculty = '" + faculty_code + "' "
				+ ((faculty_code.equals("90")) ? " and crsno in (" + getGroupCrsno(lineCode) + ") " : "");

		return sql;
	}

	public int SummerFacultyCombine(DBManager dbManager, Connection conn, String AYEAR, String SMS, String stno, String faculty_code) throws Exception {
		return SummerFacultyCombine(dbManager, conn, AYEAR, SMS, stno, faculty_code, 0);
	}

	//抵免學分數-學系開設
	public int ReduceFacultyMajor(DBManager dbManager, Connection conn, String AYEAR, String SMS, String stno, String faculty_code, int lineCode) throws Exception {
		DBResult	rs	=	null;
		int rtnValue = 0;
		try {
			GRAT004DAO	GRAT004DAO	=	new GRAT004DAO(dbManager, conn);
			//System.out.println(lineCode);
			//System.out.println(getGroupCrsno(lineCode));

			int digitOne = lineCode / 10;
			if("90".equals(faculty_code)){
				digitOne = 0;
			}
			String sql = "select sum(crd) as total from ( " +
						"SELECT * FROM ("+GradUtil.getAdvCrdBankForCrdAdopt(stno)+") WHERE sms in ('1', '2') and crsno not in (select a.crsno from grat027 a where a.kind = '"+digitOne+"') and stno = '" + stno + "' " +
						((!ht.get("STTYPE").equals("1") || !ht.get("TIMES").equals("0"))?
						"AND ayear || DECODE (sms, '1', '2', '2', '3', '3', '1') <= " +
	          				"(SELECT ayear || DECODE (sms, '1', '2', '2', '3', '3', '1') FROM grat003 WHERE stno = '" + stno + "' " +
	              				"AND num_of_times = (SELECT num_of_times FROM grat003 WHERE ayear = '" + AYEAR + "' AND sms = '" + SMS + "' AND stno = '" + stno + "')  " +
				  		"AND app_grad_type IN ('01', '02', '05') AND GRAD_REEXAM_STATUS <> '3') " +
	   					"AND ayear || DECODE (sms, '1', '2', '2', '3', '3', '1') > " +
	          				"DECODE ((SELECT num_of_times FROM grat003 WHERE ayear = '" + AYEAR + "' AND sms = '" + SMS + "' AND stno = '" + stno + "'), " +
	                  			"'1', 0, (SELECT ayear || DECODE (sms, '1', '2', '2', '3', '3', '1') FROM grat003 " +
					  	" WHERE stno = '" + stno + "' AND num_of_times = (SELECT num_of_times - 1 FROM grat003 WHERE ayear = '" + AYEAR + "' AND sms = '" + SMS + "' AND stno = '" + stno + "') AND app_grad_type IN ('01', '02', '05') AND GRAD_REEXAM_STATUS <> '3')) ":"") +

						") a inner join (select distinct crsno from cout103 where total_crs_no = '01' and crs_group_code = '002' and faculty_code = '" + faculty_code + "') b on a.crsno = b.crsno and a.get_manner = '2' " +
						((faculty_code.equals("90"))?" and a.crsno in (" + getGroupCrsno(lineCode) + ") ":"") +
						((!faculty_code.equals("90"))?"AND A.CRSNO NOT IN ( " +
						"		SELECT A.CRSNO FROM ("+GradUtil.getAdvCrdBankForCrdAdopt(stno)+") A JOIN (select distinct crsno from cout103 where total_crs_no = '01' and crs_group_code = '002' and faculty_code = '90') B ON A.CRSNO = B.CRSNO " +
						"		  WHERE A.STNO = '" + stno + "' " +
						"		) ":"") +
						((ht.get("STTYPE").equals("2") || ht.get("STTYPE").equals("3") || ht.get("FLAG").equals("Y"))?"and a.crsno not in (SELECT CRSNO FROM grat004 a where a.is_summer = 'N' AND a.stno = '" + stno + "') ":"") ;

			String sql2 = "SELECT '" + ht.get("AYEAR") + "' ayear, '" + ht.get("SMS") + "' as sms, stno, '" + ht.get("STTYPE") + "' as kind, decode('" + faculty_code + "', '90', '" + lineCode%10 + "', '0') group_code, decode('" + faculty_code + "', '90', '0', '10', '1', '20', '2', '30', '3', '40', '4', '50', '5', '60', '6', '') faculty_code, a.crsno, '000' as get_ayear, '0' as get_sms, crd, '' get_manner, '' is_valid, '' upd_user_id, '' upd_date, '' upd_time, '' upd_mk, '' rowstamp from ( " +
						"SELECT * FROM ("+GradUtil.getAdvCrdBankForCrdAdopt(stno)+") WHERE sms in ('1', '2') and crsno not in (select a.crsno from grat027 a where a.kind = '"+digitOne+"') and stno = '" + stno + "' " +
						((!ht.get("STTYPE").equals("1") || !ht.get("TIMES").equals("0"))?
						"AND ayear || DECODE (sms, '1', '2', '2', '3', '3', '1') <= " +
			  				"(SELECT ayear || DECODE (sms, '1', '2', '2', '3', '3', '1') FROM grat003 WHERE stno = '" + stno + "' " +
			      				"AND num_of_times = (SELECT num_of_times FROM grat003 WHERE ayear = '" + AYEAR + "' AND sms = '" + SMS + "' AND stno = '" + stno + "')  " +
				  		"AND app_grad_type IN ('01', '02', '05') AND GRAD_REEXAM_STATUS <> '3') " +
							"AND ayear || DECODE (sms, '1', '2', '2', '3', '3', '1') > " +
			  				"DECODE ((SELECT num_of_times FROM grat003 WHERE ayear = '" + AYEAR + "' AND sms = '" + SMS + "' AND stno = '" + stno + "'), " +
			          			"'1', 0, (SELECT ayear || DECODE (sms, '1', '2', '2', '3', '3', '1') FROM grat003 " +
					  	" WHERE stno = '" + stno + "' AND num_of_times = (SELECT num_of_times - 1 FROM grat003 WHERE ayear = '" + AYEAR + "' AND sms = '" + SMS + "' AND stno = '" + stno + "') AND app_grad_type IN ('01', '02', '05') AND GRAD_REEXAM_STATUS <> '3')) ":"") +

						") a inner join (select distinct crsno from cout103 where total_crs_no = '01' and crs_group_code = '002' and faculty_code = '" + faculty_code + "') b on a.crsno = b.crsno and a.get_manner = '2' " +
						((faculty_code.equals("90"))?" and a.crsno in (" + getGroupCrsno(lineCode) + ") ":"") +
						((!faculty_code.equals("90"))?"AND A.CRSNO NOT IN ( " +
						"		SELECT A.CRSNO FROM ("+GradUtil.getAdvCrdBankForCrdAdopt(stno)+") A JOIN (select distinct crsno from cout103 where total_crs_no = '01' and crs_group_code = '002' and faculty_code = '90') B ON A.CRSNO = B.CRSNO " +
						"		  WHERE A.STNO = '" + stno + "' " +
						"		) ":"") +
						((ht.get("STTYPE").equals("2") || ht.get("STTYPE").equals("3") || ht.get("FLAG").equals("Y"))?"and a.crsno not in (SELECT CRSNO FROM grat004 a where a.is_summer = 'N' AND a.stno = '" + stno + "') ":"") ;

			//System.out.println("ReduceFacultyMajor:"+sql);
			rs	=	GRAT004DAO.query(sql);
			try {
				if(ht.get("stnoOrNot").equals("true")) {
					GRAT004DAO.execute("insert into grat033 (" + sql2 + ")");
				}
			} catch(Exception e) {
				//System.out.println(e);
			}

			if (rs.next()) {
				rtnValue = rs.getInt("TOTAL");
			}
			return rtnValue;
		} catch(Exception e) {
			throw e;
		} finally {
			if (rs != null)
				rs.close();
		}
	}

	/**
	 * 抵免學分數-學系開設
	 *
	 * @param AYEAR
	 * @param SMS
	 * @param stno
	 * @param faculty_code
	 * @param lineCode
	 * @return
	 * @throws Exception
	 */
	public String ReduceFacultyMajorStrOnly(String AYEAR, String SMS, String stno, String faculty_code, int lineCode) throws Exception {
		int digitOne = lineCode / 10;
		if ("90".equals(faculty_code)) {
			digitOne = 0;
		}
		String sql = "select sum(crd) as total from ( "
				+ "SELECT * FROM ("
				+ GradUtil.getAdvCrdBankForCrdAdopt(stno)
				+ ") WHERE sms in ('1', '2') and crsno not in (select a.crsno from grat027 a where a.kind = '"
				+ digitOne
				+ "') and stno = '"
				+ stno
				+ "' "
				+ ((!ht.get("STTYPE").equals("1") || !ht.get("TIMES").equals("0")) ? "AND ayear || DECODE (sms, '1', '2', '2', '3', '3', '1') <= "
						+ "(SELECT ayear || DECODE (sms, '1', '2', '2', '3', '3', '1') FROM grat003 WHERE stno = '"
						+ stno
						+ "' "
						+ "AND num_of_times = (SELECT num_of_times FROM grat003 WHERE ayear = '"
						+ AYEAR
						+ "' AND sms = '"
						+ SMS
						+ "' AND stno = '"
						+ stno
						+ "')  "
						+ "AND app_grad_type IN ('01', '02', '05') AND GRAD_REEXAM_STATUS <> '3') "
						+ "AND ayear || DECODE (sms, '1', '2', '2', '3', '3', '1') > "
						+ "DECODE ((SELECT num_of_times FROM grat003 WHERE ayear = '"
						+ AYEAR
						+ "' AND sms = '"
						+ SMS
						+ "' AND stno = '"
						+ stno
						+ "'), "
						+ "'1', 0, (SELECT ayear || DECODE (sms, '1', '2', '2', '3', '3', '1') FROM grat003 "
						+ " WHERE stno = '"
						+ stno
						+ "' AND num_of_times = (SELECT num_of_times - 1 FROM grat003 WHERE ayear = '"
						+ AYEAR
						+ "' AND sms = '"
						+ SMS
						+ "' AND stno = '"
						+ stno + "') AND app_grad_type IN ('01', '02', '05') AND GRAD_REEXAM_STATUS <> '3')) " : "")
				+

				") a inner join (select distinct crsno from cout103 where total_crs_no = '01' and crs_group_code = '002' and faculty_code = '"
				+ faculty_code
				+ "') b on a.crsno = b.crsno and a.get_manner = '2' "
				+ ((faculty_code.equals("90")) ? " and a.crsno in (" + getGroupCrsno(lineCode) + ") " : "")
				+ ((!faculty_code.equals("90")) ? "AND A.CRSNO NOT IN ( "
						+ "		SELECT A.CRSNO FROM ("
						+ GradUtil.getAdvCrdBankForCrdAdopt(stno)
						+ ") A JOIN (select distinct crsno from cout103 where total_crs_no = '01' and crs_group_code = '002' and faculty_code = '90') B ON A.CRSNO = B.CRSNO "
						+ "		  WHERE A.STNO = '" + stno + "' " + "		) "
						: "")
				+ ((ht.get("STTYPE").equals("2") || ht.get("STTYPE").equals("3") || ht.get("FLAG").equals("Y")) ? "and a.crsno not in (SELECT CRSNO FROM grat004 a where a.is_summer = 'N' AND a.stno = '"
						+ stno + "') "
						: "");

		return sql;
	}

	public int ReduceFacultyMajor(DBManager dbManager, Connection conn, String AYEAR, String SMS, String stno, String faculty_code) throws Exception {
		return ReduceFacultyMajor(dbManager, conn, AYEAR, SMS, stno, faculty_code, 0);
	}

	//抵免學分數-申請歸併
	public int ReduceFacultyCombin(DBManager dbManager, Connection conn, String AYEAR, String SMS, String stno, String faculty_code, int lineCode) throws Exception {
		DBResult	rs	=	null;
		int rtnValue = 0;
		try {
			GRAT004DAO	GRAT004DAO	=	new GRAT004DAO(dbManager, conn);

			String sql = "select sum(b.crd) as total from ( " +
						"SELECT * FROM ("+GradUtil.getAdvCrdBankForCrdAdopt(stno)+") WHERE stno = '" + stno + "' " +
						((!ht.get("STTYPE").equals("1") || !ht.get("TIMES").equals("0"))?
	   					"AND ayear || DECODE (sms, '1', '2', '2', '3', '3', '1') <= " +
	          				"(SELECT ayear || DECODE (sms, '1', '2', '2', '3', '3', '1') FROM grat003 WHERE stno = '" + stno + "' " +
	              				"AND num_of_times = (SELECT num_of_times FROM grat003 WHERE ayear = '" + AYEAR + "' AND sms = '" + SMS + "' AND stno = '" + stno + "')  " +
				  		"AND app_grad_type IN ('01', '02', '05') AND GRAD_REEXAM_STATUS <> '3') " +
	   					"AND ayear || DECODE (sms, '1', '2', '2', '3', '3', '1') > " +
	          				"DECODE ((SELECT num_of_times FROM grat003 WHERE ayear = '" + AYEAR + "' AND sms = '" + SMS + "' AND stno = '" + stno + "'), " +
	                  			"'1', 0, (SELECT ayear || DECODE (sms, '1', '2', '2', '3', '3', '1') FROM grat003  " +
					  	"WHERE stno = '" + stno + "' AND num_of_times = (SELECT num_of_times - 1 FROM grat003 WHERE ayear = '" + AYEAR + "' AND sms = '" + SMS + "' AND stno = '" + stno + "') AND app_grad_type IN ('01', '02', '05') AND GRAD_REEXAM_STATUS <> '3')) ":"") +

						") a inner join grat004 b on a.crsno = b.crsno and a.get_manner = '2' and b.adopt_faculty = '" + faculty_code + "' " +
						((faculty_code.equals("90"))?" and a.crsno in (" + getGroupCrsno(lineCode) + ") ":"") +
						"and a.stno = b.stno and b.is_summer = 'N'";

			String sql2 = "SELECT '" + ht.get("AYEAR") + "' ayear, '" + ht.get("SMS") + "' as sms, a.stno, '" + ht.get("STTYPE") + "' as kind, decode('" + faculty_code + "', '90', '" + lineCode%10 + "', '0') group_code, decode('" + faculty_code + "', '90', '0', '10', '1', '20', '2', '30', '3', '40', '4', '50', '5', '60', '6', '') faculty_code, a.crsno, '000' as  get_ayear, '0' as get_sms, a.crd, '' get_manner, '' is_valid, '' upd_user_id, '' upd_date, '' upd_time, '' upd_mk, '' rowstamp from ( " +
						"SELECT * FROM ("+GradUtil.getAdvCrdBankForCrdAdopt(stno)+") WHERE stno = '" + stno + "' " +
						((!ht.get("STTYPE").equals("1") || !ht.get("TIMES").equals("0"))?
							"AND ayear || DECODE (sms, '1', '2', '2', '3', '3', '1') <= " +
			  				"(SELECT ayear || DECODE (sms, '1', '2', '2', '3', '3', '1') FROM grat003 WHERE stno = '" + stno + "' " +
			      				"AND num_of_times = (SELECT num_of_times FROM grat003 WHERE ayear = '" + AYEAR + "' AND sms = '" + SMS + "' AND stno = '" + stno + "')  " +
				  		"AND app_grad_type IN ('01', '02', '05') AND GRAD_REEXAM_STATUS <> '3') " +
							"AND ayear || DECODE (sms, '1', '2', '2', '3', '3', '1') > " +
			  				"DECODE ((SELECT num_of_times FROM grat003 WHERE ayear = '" + AYEAR + "' AND sms = '" + SMS + "' AND stno = '" + stno + "'), " +
			          			"'1', 0, (SELECT ayear || DECODE (sms, '1', '2', '2', '3', '3', '1') FROM grat003  " +
					  	"WHERE stno = '" + stno + "' AND num_of_times = (SELECT num_of_times - 1 FROM grat003 WHERE ayear = '" + AYEAR + "' AND sms = '" + SMS + "' AND stno = '" + stno + "') AND app_grad_type IN ('01', '02', '05') AND GRAD_REEXAM_STATUS <> '3')) ":"") +

						") a inner join grat004 b on a.crsno = b.crsno and a.get_manner = '2' and b.adopt_faculty = '" + faculty_code + "' " +
						((faculty_code.equals("90"))?" and a.crsno in (" + getGroupCrsno(lineCode) + ") ":"") +
						"and a.stno = b.stno and b.is_summer = 'N'";

			//System.out.println("ReduceFacultyCombin:"+sql);
			//System.out.println("ReduceFacultyCombin:"+sql2);


			rs	=	GRAT004DAO.query(sql);
			try {
				if(ht.get("stnoOrNot").equals("true")) {
					GRAT004DAO.execute("insert into grat033 (" + sql2 + ")");
				}
			} catch(Exception e) {
				//System.out.println(e);
			}

			if (rs.next()) {
				rtnValue = rs.getInt("TOTAL");
			}
			return rtnValue;
		} catch(Exception e) {
			throw e;
		} finally {
			if (rs != null)
				rs.close();
		}
	}

	/**
	 * 抵免學分數-申請歸併
	 *
	 * @param AYEAR
	 * @param SMS
	 * @param stno
	 * @param faculty_code
	 * @param lineCode
	 * @return
	 * @throws Exception
	 */
	public String ReduceFacultyCombinStrOnly(String AYEAR, String SMS, String stno, String faculty_code, int lineCode) throws Exception {
		String sql = "select sum(b.crd) as total from ( "
				+ "SELECT * FROM ("
				+ GradUtil.getAdvCrdBankForCrdAdopt(stno)
				+ ") WHERE stno = '"
				+ stno
				+ "' "
				+ ((!ht.get("STTYPE").equals("1") || !ht.get("TIMES").equals("0")) ? "AND ayear || DECODE (sms, '1', '2', '2', '3', '3', '1') <= "
						+ "(SELECT ayear || DECODE (sms, '1', '2', '2', '3', '3', '1') FROM grat003 WHERE stno = '"
						+ stno
						+ "' "
						+ "AND num_of_times = (SELECT num_of_times FROM grat003 WHERE ayear = '"
						+ AYEAR
						+ "' AND sms = '"
						+ SMS
						+ "' AND stno = '"
						+ stno
						+ "')  "
						+ "AND app_grad_type IN ('01', '02', '05') AND GRAD_REEXAM_STATUS <> '3') "
						+ "AND ayear || DECODE (sms, '1', '2', '2', '3', '3', '1') > "
						+ "DECODE ((SELECT num_of_times FROM grat003 WHERE ayear = '"
						+ AYEAR
						+ "' AND sms = '"
						+ SMS
						+ "' AND stno = '"
						+ stno
						+ "'), "
						+ "'1', 0, (SELECT ayear || DECODE (sms, '1', '2', '2', '3', '3', '1') FROM grat003  "
						+ "WHERE stno = '"
						+ stno
						+ "' AND num_of_times = (SELECT num_of_times - 1 FROM grat003 WHERE ayear = '"
						+ AYEAR
						+ "' AND sms = '"
						+ SMS
						+ "' AND stno = '"
						+ stno + "') AND app_grad_type IN ('01', '02', '05') AND GRAD_REEXAM_STATUS <> '3')) " : "") +

				") a inner join grat004 b on a.crsno = b.crsno and a.get_manner = '2' and b.adopt_faculty = '" + faculty_code + "' "
				+ ((faculty_code.equals("90")) ? " and a.crsno in (" + getGroupCrsno(lineCode) + ") " : "") + "and a.stno = b.stno and b.is_summer = 'N'";

		return sql;
	}

	public int ReduceFacultyCombin(DBManager dbManager, Connection conn, String AYEAR, String SMS, String stno, String faculty_code) throws Exception {
		return ReduceFacultyCombin(dbManager, conn, AYEAR, SMS, stno, faculty_code, 0);
	}

	public void CrdSum(int[] CRD) throws Exception {

		//直接相加得到結果的部份開始
		//累計共同課程總學分
		CRD[23] = CRD[13] + CRD[14] + CRD[17] + CRD[18] + CRD[21] + CRD[22];
		CRD[38] = CRD[28] + CRD[29] + CRD[32] + CRD[33] + CRD[36] + CRD[37];
		CRD[52] = CRD[42] + CRD[43] + CRD[46] + CRD[47] + CRD[50] + CRD[51];
		CRD[66] = CRD[56] + CRD[57] + CRD[60] + CRD[61] + CRD[64] + CRD[65];
		CRD[80] = CRD[70] + CRD[71] + CRD[74] + CRD[75] + CRD[78] + CRD[79];
		CRD[94] = CRD[84] + CRD[85] + CRD[88] + CRD[89] + CRD[92] + CRD[93];

		//審核用才會顯示
		if ("2".equals(ht.get("STTYPE")) || ht.get("STTYPE").equals("3")|| ht.get("FLAG").equals("Y")) {
			//採計共同課程總學分
			CRD[24] = CRD[13] + CRD[14] + CRD[19] + CRD[20] + CRD[21] + CRD[22];
			CRD[39] = CRD[28] + CRD[29] + CRD[34] + CRD[35] + CRD[36] + CRD[37];
			CRD[53] = CRD[42] + CRD[43] + CRD[48] + CRD[49] + CRD[50] + CRD[51];
			CRD[67] = CRD[56] + CRD[57] + CRD[62] + CRD[63] + CRD[64] + CRD[65];
			CRD[81] = CRD[70] + CRD[71] + CRD[76] + CRD[77] + CRD[78] + CRD[79];
			CRD[95] = CRD[84] + CRD[85] + CRD[90] + CRD[91] + CRD[92] + CRD[93];

			//採計學系主修總學分
			CRD[108] = CRD[97] + CRD[98] + CRD[103] + CRD[104] + CRD[105] + CRD[106];
			CRD[121] = CRD[110] + CRD[111] + CRD[116] + CRD[117] + CRD[118] + CRD[119];
			CRD[135] = CRD[124] + CRD[125] + CRD[130] + CRD[131] + CRD[132] + CRD[133];
			CRD[148] = CRD[137] + CRD[138] + CRD[143] + CRD[144] + CRD[145] + CRD[146];
			CRD[161] = CRD[150] + CRD[151] + CRD[156] + CRD[157] + CRD[158] + CRD[159];
			CRD[174] = CRD[163] + CRD[164] + CRD[169] + CRD[170] + CRD[171] + CRD[172];

		}

		//累計學系主修總學分
		CRD[107] = CRD[97] + CRD[98] + CRD[101] + CRD[102] + CRD[105] + CRD[106];
		CRD[120] = CRD[110] + CRD[111] + CRD[114] + CRD[115] + CRD[118] + CRD[119];
		CRD[134] = CRD[124] + CRD[125] + CRD[128] + CRD[129] + CRD[132] + CRD[133];
		CRD[147] = CRD[137] + CRD[138] + CRD[141] + CRD[142] + CRD[145] + CRD[146];
		CRD[160] = CRD[150] + CRD[151] + CRD[154] + CRD[155] + CRD[158] + CRD[159];
		CRD[173] = CRD[163] + CRD[164] + CRD[167] + CRD[168] + CRD[171] + CRD[172];

		CRD[176] = CRD[13] + CRD[14] + CRD[28] + CRD[29] + CRD[42] + CRD[43] + CRD[56] + CRD[57] + CRD[70] + CRD[71] + CRD[84] + CRD[85] + CRD[97] + CRD[98] + CRD[110] + CRD[111] + CRD[124] + CRD[125] + CRD[137] + CRD[138] + CRD[150] + CRD[151] + CRD[163] + CRD[164];
		CRD[177] = CRD[15] + CRD[16] + CRD[30] + CRD[31] + CRD[44] + CRD[45] + CRD[58] + CRD[59] + CRD[72] + CRD[73] + CRD[86] + CRD[87] + CRD[99] + CRD[100] + CRD[112] + CRD[113] + CRD[126] + CRD[127] + CRD[139] + CRD[140] + CRD[152] + CRD[153] + CRD[165] + CRD[166];
		CRD[178] = CRD[17] + CRD[18] + CRD[32] + CRD[33] + CRD[46] + CRD[47] + CRD[60] + CRD[61] + CRD[74] + CRD[75] + CRD[88] + CRD[89] + CRD[101] + CRD[102] + CRD[114] + CRD[115] + CRD[128] + CRD[129] + CRD[141] + CRD[142] + CRD[154] + CRD[155] + CRD[167] + CRD[168];
		CRD[179] = CRD[19] + CRD[20] + CRD[34] + CRD[35] + CRD[48] + CRD[49] + CRD[62] + CRD[63] + CRD[76] + CRD[77] + CRD[90] + CRD[91] + CRD[103] + CRD[104] + CRD[116] + CRD[117] + CRD[130] + CRD[131] + CRD[143] + CRD[144] + CRD[156] + CRD[157] + CRD[169] + CRD[170];
		CRD[180] = CRD[21] + CRD[22] + CRD[36] + CRD[37] + CRD[50] + CRD[51] + CRD[64] + CRD[65] + CRD[78] + CRD[79] + CRD[92] + CRD[93] + CRD[105] + CRD[106] + CRD[118] + CRD[119] + CRD[132] + CRD[133] + CRD[145] + CRD[146] + CRD[158] + CRD[159] + CRD[171] + CRD[172];
		CRD[182] = CRD[23] + CRD[38] + CRD[52] + CRD[66] + CRD[80] + CRD[94] + CRD[107] + CRD[120] + CRD[134] + CRD[147] + CRD[160] + CRD[173];
		CRD[183] = CRD[24] + CRD[39] + CRD[53] + CRD[67] + CRD[81] + CRD[95] + CRD[108] + CRD[121] + CRD[135] + CRD[148] + CRD[161] + CRD[174];
//		if(CRD[183] < 128){
//			CRD[184] = -1;
//		}
		//直接相加得到結果的部份結束

	}

	/**
	 * 執行多筆時採用將所有SQL組成一句超大SQL, 以提高效能
	 *
	 * @param dbManager
	 * @param conn
	 * @param AYEAR
	 * @param SMS
	 * @param stno
	 * @param CRD
	 * @param mainAyearSms
	 * @throws Exception
	 */
	public void runBatchForCrd(DBManager dbManager, Connection conn, String AYEAR, String SMS, String stno,int[] CRD, String mainAyearSms) throws Exception{
		String type = rtnType(mainAyearSms);
		String[] positions = new String[] { "13", "28", "42", "56", "70", "84", "97", "110", "124", "137", "150", "163", "14", "29", "43", "57", "71", "85",
				"98", "111", "125", "138", "151", "164", "15", "30", "44", "58", "72", "86", "99", "112", "126", "139", "152", "165", "16", "31", "45", "59",
				"73", "87", "100", "113", "127", "140", "153", "166", "17", "32", "46", "60", "74", "88", "101", "114", "128", "141", "154", "167", "18", "33",
				"47", "61", "75", "89", "102", "115", "129", "142", "155", "168", "19", "34", "48", "62", "76", "90", "103", "116", "130", "143", "156", "169",
				"20", "35", "49", "63", "77", "91", "104", "117", "131", "144", "157", "170", "21", "36", "50", "64", "78", "92", "105", "118", "132", "145",
				"158", "171", "22", "37", "51", "65", "79", "93", "106", "119", "133", "146", "159", "172" };

		Map facultyCode = this.getFacultyCode();
		StringBuffer allSqls = new StringBuffer();
		allSqls.append("select ");

		int index = 0;
		int fIndex = 0;
		for (int i = 0; i < positions.length; i++) {
			if (i != 0) {
				allSqls.append(",");
			}

			allSqls.append(" nvl(( ");

			if (i >= 0 && i < 6) {
				allSqls.append(this.FacultyMajorStrOnly(AYEAR, SMS, stno, "90", Integer.parseInt(type + String.valueOf(index + 1))));
				index++;
				fIndex = 0;
			} else if (i >= 6 && i < 12) {
				allSqls.append(this.FacultyMajorTempStrOnly(AYEAR, SMS, stno, (String) facultyCode.get(String.valueOf(fIndex)), 0));
				index = 0;
				fIndex++;
			} else if (i >= 12 && i < 18) {
				allSqls.append(this.FacultyCombineStrOnly(AYEAR, SMS, stno, "90", Integer.parseInt(type + String.valueOf(index + 1))));
				index++;
				fIndex = 0;
			} else if (i >= 18 && i < 24) {
				allSqls.append(this.FacultyCombineStrOnly(AYEAR, SMS, stno, (String) facultyCode.get(String.valueOf(fIndex)), 0));
				index = 0;
				fIndex++;
			} else if (i >= 24 && i < 30) {
				allSqls.append(this.ThisSMSFacultyMajorStrOnly(AYEAR, SMS, stno, "90", Integer.parseInt(type + String.valueOf(index + 1))));
				index++;
				fIndex = 0;
			} else if (i >= 30 && i < 36) {
				allSqls.append(this.ThisSMSFacultyMajorStrOnly(AYEAR, SMS, stno, (String) facultyCode.get(String.valueOf(fIndex)), 0));
				index = 0;
				fIndex++;
			} else if (i >= 36 && i < 42) {
				allSqls.append(this.ThisSMSFacultyCombineStrOnly(AYEAR, SMS, stno, "90", Integer.parseInt(type + String.valueOf(index + 1))));
				index++;
				fIndex = 0;
			} else if (i >= 42 && i < 48) {
				allSqls.append(this.ThisSMSFacultyCombineStrOnly(AYEAR, SMS, stno, (String) facultyCode.get(String.valueOf(fIndex)), 0));
				index = 0;
				fIndex++;
			} else if (i >= 48 && i < 54) {
				allSqls.append(this.SummerAllFacultyMajorStrOnly(AYEAR, SMS, stno, "90", Integer.parseInt(type + String.valueOf(index + 1))));
				index++;
				fIndex = 0;
			} else if (i >= 54 && i < 60) {
				allSqls.append(this.SummerAllFacultyMajorStrOnly(AYEAR, SMS, stno, (String) facultyCode.get(String.valueOf(fIndex)), 0));
				index = 0;
				fIndex++;
			} else if (i >= 60 && i < 66) {
				allSqls.append(this.SummerFacultyCombineStrOnly(AYEAR, SMS, stno, "90", Integer.parseInt(type + String.valueOf(index + 1))));
				index++;
				fIndex = 0;
			} else if (i >= 66 && i < 72) {
				allSqls.append(this.SummerFacultyCombineStrOnly(AYEAR, SMS, stno, (String) facultyCode.get(String.valueOf(fIndex)), 0));
				index = 0;
				fIndex++;
			} else if (i >= 72 && i < 78) {
				allSqls.append(this.SummerFacultyMajorStrOnly(AYEAR, SMS, stno, "90", Integer.parseInt(type + String.valueOf(index + 1))));
				index++;
				fIndex = 0;
			} else if (i >= 78 && i < 84) {
				allSqls.append(this.SummerFacultyMajorStrOnly(AYEAR, SMS, stno, (String) facultyCode.get(String.valueOf(fIndex)), 0));
				index = 0;
				fIndex++;
			} else if (i >= 84 && i < 90) {
				allSqls.append(this.SummerFacultyCombineStrOnly(AYEAR, SMS, stno, "90", Integer.parseInt(type + String.valueOf(index + 1))));
				index++;
				fIndex = 0;
			} else if (i >= 90 && i < 96) {
				allSqls.append(this.SummerFacultyCombineStrOnly(AYEAR, SMS, stno, (String) facultyCode.get(String.valueOf(fIndex)), 0));
				index = 0;
				fIndex++;
			} else if (i >= 96 && i < 102) {
				allSqls.append(this.ReduceFacultyMajorStrOnly(AYEAR, SMS, stno, "90", Integer.parseInt(type + String.valueOf(index + 1))));
				index++;
				fIndex = 0;
			} else if (i >= 102 && i < 108) {
				allSqls.append(this.ReduceFacultyMajorStrOnly(AYEAR, SMS, stno, (String) facultyCode.get(String.valueOf(fIndex)), 0));
				index = 0;
				fIndex++;
			} else if (i >= 108 && i < 114) {
				allSqls.append(this.ReduceFacultyCombinStrOnly(AYEAR, SMS, stno, "90", Integer.parseInt(type + String.valueOf(index + 1))));
				index++;
				fIndex = 0;
			} else if (i >= 114 && i < 120) {
				allSqls.append(this.ReduceFacultyCombinStrOnly(AYEAR, SMS, stno, (String) facultyCode.get(String.valueOf(fIndex)), 0));
				index = 0;
				fIndex++;
			} else {
				allSqls.append(this.getReduceCRDStrOnly(AYEAR, SMS, stno));
			}

			allSqls.append(" ), 0) as total_");
			allSqls.append(positions[i]);
		}
		allSqls.append(" from dual ");

		//System.out.println(allSqls.toString());

		DBResult rs = null;
		try {
			rs = dbManager.getSimpleResultSet(conn);
			rs.open();
			rs.executeQuery(allSqls.toString());

			while (rs.next()) {
				for (int i = 1; i <= rs.getColumnCount(); i++) {
					String[] temp = rs.getColumnName(i).split("_");
					CRD[Integer.parseInt(temp[1])] = Integer.parseInt(rs.getString(i));
				}
			}
		} catch (Exception e) {
			throw e;
		} finally {
			if (rs != null)
				rs.close();
		}
	}

	public void DoFacultyMajor(DBManager dbManager, Connection conn, String AYEAR, String SMS, String stno,int[] CRD, String mainAyearSms) throws Exception {
		String type = rtnType(mainAyearSms);

		CRD[13] = FacultyMajor(dbManager, conn, AYEAR, SMS, stno, "90", Integer.parseInt(type + "1"));
		CRD[28] = FacultyMajor(dbManager, conn, AYEAR, SMS, stno, "90", Integer.parseInt(type + "2"));
		CRD[42] = FacultyMajor(dbManager, conn, AYEAR, SMS, stno, "90", Integer.parseInt(type + "3"));
		CRD[56] = FacultyMajor(dbManager, conn, AYEAR, SMS, stno, "90", Integer.parseInt(type + "4"));
		CRD[70] = FacultyMajor(dbManager, conn, AYEAR, SMS, stno, "90", Integer.parseInt(type + "5"));
		CRD[84] = FacultyMajor(dbManager, conn, AYEAR, SMS, stno, "90", Integer.parseInt(type + "6"));

		Map map = this.getPosition();
		Iterator iter = map.keySet().iterator();
		while (iter.hasNext()) {
			String key = (String) iter.next();
			CRD[((Integer) map.get(key)).intValue()] = this.FacultyMajorTemp(dbManager, conn, AYEAR, SMS, stno, key, 0);
		}
// 		CRD[97] = FacultyMajor(dbManager, conn, AYEAR, SMS, stno, "10");
//		CRD[110] = FacultyMajor(dbManager, conn, AYEAR, SMS, stno, "20");
//		CRD[124] = FacultyMajor(dbManager, conn, AYEAR, SMS, stno, "30");
//		CRD[137] = FacultyMajor(dbManager, conn, AYEAR, SMS, stno, "40");
//		CRD[150] = FacultyMajor(dbManager, conn, AYEAR, SMS, stno, "50");
//		CRD[163] = FacultyMajor(dbManager, conn, AYEAR, SMS, stno, "60");
	}

	private Map getFacultyCode(){
		Map map = new LinkedHashMap();
		map.put("0", "10");
		map.put("1", "20");
		map.put("2", "30");
		map.put("3", "40");
		map.put("4", "50");
		map.put("5", "60");
		return map;
	}

	private Map getPosition(){
		Map map = new LinkedHashMap();
		map.put("10", new Integer(97));
		map.put("20", new Integer(110));
		map.put("30", new Integer(124));
		map.put("40", new Integer(137));
		map.put("50", new Integer(150));
		map.put("60", new Integer(163));
		return map;
	}

	//OK
	public void DoFacultyCombin(DBManager dbManager, Connection conn, String AYEAR, String SMS, String stno,int[] CRD, String mainAyearSms) throws Exception {
		String type = rtnType(mainAyearSms);

		if ("2".equals(ht.get("STTYPE")) || ht.get("STTYPE").equals("3")|| ht.get("FLAG").equals("Y")) {
			CRD[14]  = FacultyCombine(dbManager, conn, AYEAR, SMS, stno, "90", Integer.parseInt(type + "1"));
			CRD[29]  = FacultyCombine(dbManager, conn, AYEAR, SMS, stno, "90", Integer.parseInt(type + "2"));
			CRD[43]  = FacultyCombine(dbManager, conn, AYEAR, SMS, stno, "90", Integer.parseInt(type + "3"));
			CRD[57]  = FacultyCombine(dbManager, conn, AYEAR, SMS, stno, "90", Integer.parseInt(type + "4"));
			CRD[71]  = FacultyCombine(dbManager, conn, AYEAR, SMS, stno, "90", Integer.parseInt(type + "5"));
			CRD[85]  = FacultyCombine(dbManager, conn, AYEAR, SMS, stno, "90", Integer.parseInt(type + "6"));
			CRD[98]  = FacultyCombine(dbManager, conn, AYEAR, SMS, stno, "10");
			CRD[111] = FacultyCombine(dbManager, conn, AYEAR, SMS, stno, "20");
			CRD[125] = FacultyCombine(dbManager, conn, AYEAR, SMS, stno, "30");
			CRD[138] = FacultyCombine(dbManager, conn, AYEAR, SMS, stno, "40");
			CRD[151] = FacultyCombine(dbManager, conn, AYEAR, SMS, stno, "50");
			CRD[164] = FacultyCombine(dbManager, conn, AYEAR, SMS, stno, "60");
		}
	}

	//OK
	public void DoThisSMSFacultyMajor(DBManager dbManager, Connection conn, String AYEAR, String SMS, String stno,int[] CRD, String mainAyearSms) throws Exception {
		String type = rtnType(mainAyearSms);

		CRD[15]  = ThisSMSFacultyMajor(dbManager, conn, AYEAR, SMS, stno, "90", Integer.parseInt(type + "1"));
		CRD[30]  = ThisSMSFacultyMajor(dbManager, conn, AYEAR, SMS, stno, "90", Integer.parseInt(type + "2"));
		CRD[44]  = ThisSMSFacultyMajor(dbManager, conn, AYEAR, SMS, stno, "90", Integer.parseInt(type + "3"));
		CRD[58]  = ThisSMSFacultyMajor(dbManager, conn, AYEAR, SMS, stno, "90", Integer.parseInt(type + "4"));
		CRD[72]  = ThisSMSFacultyMajor(dbManager, conn, AYEAR, SMS, stno, "90", Integer.parseInt(type + "5"));
		CRD[86]  = ThisSMSFacultyMajor(dbManager, conn, AYEAR, SMS, stno, "90", Integer.parseInt(type + "6"));
		CRD[99]  = ThisSMSFacultyMajor(dbManager, conn, AYEAR, SMS, stno, "10");
		CRD[112] = ThisSMSFacultyMajor(dbManager, conn, AYEAR, SMS, stno, "20");
		CRD[126] = ThisSMSFacultyMajor(dbManager, conn, AYEAR, SMS, stno, "30");
		CRD[139] = ThisSMSFacultyMajor(dbManager, conn, AYEAR, SMS, stno, "40");
		CRD[152] = ThisSMSFacultyMajor(dbManager, conn, AYEAR, SMS, stno, "50");
		CRD[165] = ThisSMSFacultyMajor(dbManager, conn, AYEAR, SMS, stno, "60");
	}

	//OK
	public void DoThisSMSFacultyCombine(DBManager dbManager, Connection conn, String AYEAR, String SMS, String stno,int[] CRD, String mainAyearSms) throws Exception {
		String type = rtnType(mainAyearSms);

		if ("2".equals(ht.get("STTYPE")) || ht.get("STTYPE").equals("3")|| ht.get("FLAG").equals("Y")) {
			CRD[16]  = ThisSMSFacultyCombine(dbManager, conn, AYEAR, SMS, stno, "90", Integer.parseInt(type + "1"));
			CRD[31]  = ThisSMSFacultyCombine(dbManager, conn, AYEAR, SMS, stno, "90", Integer.parseInt(type + "2"));
			CRD[45]  = ThisSMSFacultyCombine(dbManager, conn, AYEAR, SMS, stno, "90", Integer.parseInt(type + "3"));
			CRD[59]  = ThisSMSFacultyCombine(dbManager, conn, AYEAR, SMS, stno, "90", Integer.parseInt(type + "4"));
			CRD[73]  = ThisSMSFacultyCombine(dbManager, conn, AYEAR, SMS, stno, "90", Integer.parseInt(type + "5"));
			CRD[87]  = ThisSMSFacultyCombine(dbManager, conn, AYEAR, SMS, stno, "90", Integer.parseInt(type + "6"));
			CRD[100] = ThisSMSFacultyCombine(dbManager, conn, AYEAR, SMS, stno, "10");
			CRD[113] = ThisSMSFacultyCombine(dbManager, conn, AYEAR, SMS, stno, "20");
			CRD[127] = ThisSMSFacultyCombine(dbManager, conn, AYEAR, SMS, stno, "30");
			CRD[140] = ThisSMSFacultyCombine(dbManager, conn, AYEAR, SMS, stno, "40");
			CRD[153] = ThisSMSFacultyCombine(dbManager, conn, AYEAR, SMS, stno, "50");
			CRD[166] = ThisSMSFacultyCombine(dbManager, conn, AYEAR, SMS, stno, "60");
		}
	}

	//OK
	public void DoSummerAllFacultyMajor(DBManager dbManager, Connection conn, String AYEAR, String SMS, String stno,int[] CRD, String mainAyearSms) throws Exception {
		String type = rtnType(mainAyearSms);

		CRD[17]  = SummerAllFacultyMajor(dbManager, conn, AYEAR, SMS, stno, "90", Integer.parseInt(type + "1"));
		CRD[32]  = SummerAllFacultyMajor(dbManager, conn, AYEAR, SMS, stno, "90", Integer.parseInt(type + "2"));
		CRD[46]  = SummerAllFacultyMajor(dbManager, conn, AYEAR, SMS, stno, "90", Integer.parseInt(type + "3"));
		CRD[60]  = SummerAllFacultyMajor(dbManager, conn, AYEAR, SMS, stno, "90", Integer.parseInt(type + "4"));
		CRD[74]  = SummerAllFacultyMajor(dbManager, conn, AYEAR, SMS, stno, "90", Integer.parseInt(type + "5"));
		CRD[88]  = SummerAllFacultyMajor(dbManager, conn, AYEAR, SMS, stno, "90", Integer.parseInt(type + "6"));
		CRD[101] = SummerAllFacultyMajor(dbManager, conn, AYEAR, SMS, stno, "10");
		CRD[114] = SummerAllFacultyMajor(dbManager, conn, AYEAR, SMS, stno, "20");
		CRD[128] = SummerAllFacultyMajor(dbManager, conn, AYEAR, SMS, stno, "30");
		CRD[141] = SummerAllFacultyMajor(dbManager, conn, AYEAR, SMS, stno, "40");
		CRD[154] = SummerAllFacultyMajor(dbManager, conn, AYEAR, SMS, stno, "50");
		CRD[167] = SummerAllFacultyMajor(dbManager, conn, AYEAR, SMS, stno, "60");
	}

	//OK
	public void DoSummerAllFacultyCombine(DBManager dbManager, Connection conn, String AYEAR, String SMS, String stno,int[] CRD, String mainAyearSms) throws Exception {
		String type = rtnType(mainAyearSms);

		if ("2".equals(ht.get("STTYPE")) || ht.get("STTYPE").equals("3") || ht.get("FLAG").equals("Y")) {
			CRD[18]  = SummerFacultyCombine(dbManager, conn, AYEAR, SMS, stno, "90", Integer.parseInt(type + "1"));
			CRD[33]  = SummerFacultyCombine(dbManager, conn, AYEAR, SMS, stno, "90", Integer.parseInt(type + "2"));
			CRD[47]  = SummerFacultyCombine(dbManager, conn, AYEAR, SMS, stno, "90", Integer.parseInt(type + "3"));
			CRD[61]  = SummerFacultyCombine(dbManager, conn, AYEAR, SMS, stno, "90", Integer.parseInt(type + "4"));
			CRD[75]  = SummerFacultyCombine(dbManager, conn, AYEAR, SMS, stno, "90", Integer.parseInt(type + "5"));
			CRD[89]  = SummerFacultyCombine(dbManager, conn, AYEAR, SMS, stno, "90", Integer.parseInt(type + "6"));
			CRD[102] = SummerFacultyCombine(dbManager, conn, AYEAR, SMS, stno, "10");
			CRD[115] = SummerFacultyCombine(dbManager, conn, AYEAR, SMS, stno, "20");
			CRD[129] = SummerFacultyCombine(dbManager, conn, AYEAR, SMS, stno, "30");
			CRD[142] = SummerFacultyCombine(dbManager, conn, AYEAR, SMS, stno, "40");
			CRD[155] = SummerFacultyCombine(dbManager, conn, AYEAR, SMS, stno, "50");
			CRD[168] = SummerFacultyCombine(dbManager, conn, AYEAR, SMS, stno, "60");
		}
	}

	//OK
	public void DoSummerFacultyMajor(DBManager dbManager, Connection conn, String AYEAR, String SMS, String stno,int[] CRD, String mainAyearSms) throws Exception {
		String type = rtnType(mainAyearSms);

		if ("2".equals(ht.get("STTYPE")) || ht.get("STTYPE").equals("3") || ht.get("FLAG").equals("Y")) {
			CRD[19]  = SummerFacultyMajor(dbManager, conn, AYEAR, SMS, stno, "90", Integer.parseInt(type + "1"));
			CRD[34]  = SummerFacultyMajor(dbManager, conn, AYEAR, SMS, stno, "90", Integer.parseInt(type + "2"));
			CRD[48]  = SummerFacultyMajor(dbManager, conn, AYEAR, SMS, stno, "90", Integer.parseInt(type + "3"));
			CRD[62]  = SummerFacultyMajor(dbManager, conn, AYEAR, SMS, stno, "90", Integer.parseInt(type + "4"));
			CRD[76]  = SummerFacultyMajor(dbManager, conn, AYEAR, SMS, stno, "90", Integer.parseInt(type + "5"));
			CRD[90]  = SummerFacultyMajor(dbManager, conn, AYEAR, SMS, stno, "90", Integer.parseInt(type + "6"));
			CRD[103] = SummerFacultyMajor(dbManager, conn, AYEAR, SMS, stno, "10");
			CRD[116] = SummerFacultyMajor(dbManager, conn, AYEAR, SMS, stno, "20");
			CRD[130] = SummerFacultyMajor(dbManager, conn, AYEAR, SMS, stno, "30");
			CRD[143] = SummerFacultyMajor(dbManager, conn, AYEAR, SMS, stno, "40");
			CRD[156] = SummerFacultyMajor(dbManager, conn, AYEAR, SMS, stno, "50");
			CRD[169] = SummerFacultyMajor(dbManager, conn, AYEAR, SMS, stno, "60");
		}
	}

	//OK
	public void DoSummerFacultyCombine(DBManager dbManager, Connection conn, String AYEAR, String SMS, String stno,int[] CRD, String mainAyearSms) throws Exception {
		String type = rtnType(mainAyearSms);

		if ("2".equals(ht.get("STTYPE")) || ht.get("STTYPE").equals("3") || ht.get("FLAG").equals("Y")) {
			CRD[20]  = SummerFacultyCombine(dbManager, conn, AYEAR, SMS, stno, "90", Integer.parseInt(type + "1"));
			CRD[35]  = SummerFacultyCombine(dbManager, conn, AYEAR, SMS, stno, "90", Integer.parseInt(type + "2"));
			CRD[49]  = SummerFacultyCombine(dbManager, conn, AYEAR, SMS, stno, "90", Integer.parseInt(type + "3"));
			CRD[63]  = SummerFacultyCombine(dbManager, conn, AYEAR, SMS, stno, "90", Integer.parseInt(type + "4"));
			CRD[77]  = SummerFacultyCombine(dbManager, conn, AYEAR, SMS, stno, "90", Integer.parseInt(type + "5"));
			CRD[91]  = SummerFacultyCombine(dbManager, conn, AYEAR, SMS, stno, "90", Integer.parseInt(type + "6"));
			CRD[104] = SummerFacultyCombine(dbManager, conn, AYEAR, SMS, stno, "10");
			CRD[117] = SummerFacultyCombine(dbManager, conn, AYEAR, SMS, stno, "20");
			CRD[131] = SummerFacultyCombine(dbManager, conn, AYEAR, SMS, stno, "30");
			CRD[144] = SummerFacultyCombine(dbManager, conn, AYEAR, SMS, stno, "40");
			CRD[157] = SummerFacultyCombine(dbManager, conn, AYEAR, SMS, stno, "50");
			CRD[170] = SummerFacultyCombine(dbManager, conn, AYEAR, SMS, stno, "60");
		}
	}

	//OK
	public void DoReduceFacultyMajor(DBManager dbManager, Connection conn, String AYEAR, String SMS, String stno,int[] CRD, String mainAyearSms) throws Exception {
		String type = rtnType(mainAyearSms);

		CRD[21]  = ReduceFacultyMajor(dbManager, conn, AYEAR, SMS, stno, "90", Integer.parseInt(type + "1"));
		CRD[36]  = ReduceFacultyMajor(dbManager, conn, AYEAR, SMS, stno, "90", Integer.parseInt(type + "2"));
		CRD[50]  = ReduceFacultyMajor(dbManager, conn, AYEAR, SMS, stno, "90", Integer.parseInt(type + "3"));
		CRD[64]  = ReduceFacultyMajor(dbManager, conn, AYEAR, SMS, stno, "90", Integer.parseInt(type + "4"));
		CRD[78]  = ReduceFacultyMajor(dbManager, conn, AYEAR, SMS, stno, "90", Integer.parseInt(type + "5"));
		CRD[92]  = ReduceFacultyMajor(dbManager, conn, AYEAR, SMS, stno, "90", Integer.parseInt(type + "6"));
		CRD[105] = ReduceFacultyMajor(dbManager, conn, AYEAR, SMS, stno, "10");
		CRD[118] = ReduceFacultyMajor(dbManager, conn, AYEAR, SMS, stno, "20");
		CRD[132] = ReduceFacultyMajor(dbManager, conn, AYEAR, SMS, stno, "30");
		CRD[145] = ReduceFacultyMajor(dbManager, conn, AYEAR, SMS, stno, "40");
		CRD[158] = ReduceFacultyMajor(dbManager, conn, AYEAR, SMS, stno, "50");
		CRD[171] = ReduceFacultyMajor(dbManager, conn, AYEAR, SMS, stno, "60");
	}

	//OK
	public void DoReduceFacultyCombin(DBManager dbManager, Connection conn, String AYEAR, String SMS, String stno,int[] CRD, String mainAyearSms) throws Exception {
		String type = rtnType(mainAyearSms);

		if ("2".equals(ht.get("STTYPE")) || ht.get("STTYPE").equals("3") || ht.get("FLAG").equals("Y")) {
			CRD[22]  = ReduceFacultyCombin(dbManager, conn, AYEAR, SMS, stno, "90", Integer.parseInt(type + "1"));
			CRD[37]  = ReduceFacultyCombin(dbManager, conn, AYEAR, SMS, stno, "90", Integer.parseInt(type + "2"));
			CRD[51]  = ReduceFacultyCombin(dbManager, conn, AYEAR, SMS, stno, "90", Integer.parseInt(type + "3"));
			CRD[65]  = ReduceFacultyCombin(dbManager, conn, AYEAR, SMS, stno, "90", Integer.parseInt(type + "4"));
			CRD[79]  = ReduceFacultyCombin(dbManager, conn, AYEAR, SMS, stno, "90", Integer.parseInt(type + "5"));
			CRD[93]  = ReduceFacultyCombin(dbManager, conn, AYEAR, SMS, stno, "90", Integer.parseInt(type + "6"));
			CRD[106] = ReduceFacultyCombin(dbManager, conn, AYEAR, SMS, stno, "10");
			CRD[119] = ReduceFacultyCombin(dbManager, conn, AYEAR, SMS, stno, "20");
			CRD[133] = ReduceFacultyCombin(dbManager, conn, AYEAR, SMS, stno, "30");
			CRD[146] = ReduceFacultyCombin(dbManager, conn, AYEAR, SMS, stno, "40");
			CRD[159] = ReduceFacultyCombin(dbManager, conn, AYEAR, SMS, stno, "50");
			CRD[172] = ReduceFacultyCombin(dbManager, conn, AYEAR, SMS, stno, "60");
		}
	}

	public void DoAltTable(DBManager dbManager, Connection conn, Hashtable requestMap, STUT003GATEWAY getway, RptFile rptFile, boolean is_summer, Map facultyCodeMap, Map smsMap) throws Exception {
		Vector vt2 = new Vector();
		if(is_summer) {
			requestMap.put("IS_SUMMER", "Y");
		} else {
			requestMap.put("IS_SUMMER", "N");
		}

		if (!requestMap.get("STTYPE").equals("1")) {
			getway.printStuForm203_2(vt2, requestMap);
		} else {
			getway.printStuForm203_X(vt2, requestMap);
		}

		String no184 = "";
		String no185 = "";
		String no186 = "";
		String no187 = "";
		String no188 = "";
		int no189 = 0;

		if(vt2.size() > 0 && (requestMap.get("STTYPE").equals("1") || requestMap.get("STTYPE").equals("2") || requestMap.get("STTYPE").equals("3") || requestMap.get("STTYPE").equals("4") || requestMap.get("STTYPE").equals("5") || requestMap.get("STTYPE").equals("6") || requestMap.get("STTYPE").equals("7") || "Y".equals(ht.get("FLAG")))) {
			for (int j = 0; j < vt2.size(); j++) {
				Hashtable ht2 = (Hashtable) vt2.get(j);
				no184 += ht2.get("CRSNO").toString() + "<br>";
				no185 += ht2.get("CRS_NAME").toString() + "<br>";
				no186 += ht2.get("CRD").toString() + "<br>";
				no187 += ((String) facultyCodeMap.get((String) ht2.get("ADOPT_FACULTY"))).substring(0, 1) + "<br>";
				no188 += Integer.parseInt(ht2.get("AYEAR").toString()) + ((String) smsMap.get((String) ht2.get("SMS"))).substring(0, 1) + "<br>";
				no189 += Integer.parseInt(ht2.get("CRD").toString());
			}
		}
		rptFile.add(no184+"<br>");
		rptFile.add(no185+"<br>");
		rptFile.add(no186+"<br>");
		rptFile.add(no187+"<br>");
		rptFile.add(no188+"<br>");
		rptFile.add(no189+"<br>");

	}

	public String getNumOfTimesAyearSmsStart(DBManager dbManager, Connection conn, String stno, Hashtable requestMap) throws Exception {
		DBResult	rs	=	null;
		String sql = "";
		String rtnValue = "";
		try {
			GRAT004DAO	GRAT004DAO	=	new GRAT004DAO(dbManager, conn);
			if(requestMap.get("TIMES").equals("0") && requestMap.get("STTYPE").equals("1")) {
				sql = "SELECT decode(b.ayear, null, '" + requestMap.get("AYEAR") + "', b.ayear) as ayear, decode(b.sms, null, '" + requestMap.get("SMS") + "', b.sms) as sms " +
	            	"FROM grat009 a   " +
					"LEFT JOIN (select * from grat003 where (stno || ',' || num_of_times) in (select stno || ',' || max(num_of_times) from grat003 where app_grad_type = '04' and grad_reexam_status <> '3' group by stno)) b on a.stno = b.stno and b.app_grad_type = '04' and b.grad_reexam_status <> '3'  " +
					"INNER JOIN stut003 c ON a.stno = c.stno     " +
					"INNER JOIN stut002 d ON c.idno = d.idno AND c.birthdate = d.birthdate  " +
	            	"LEFT JOIN syst005 e ON SUBSTR (c.enroll_ayearsms, 1, 3) = e.ayear  " +
	                " 		AND SUBSTR (c.enroll_ayearsms, 4, 4) = e.sms 		 " +
					"where a.ayear = '" + requestMap.get("AYEAR") + "' and a.sms = '" + requestMap.get("SMS") + "' " +
					"and a.stno = '" + stno + "' ";


			} else {
				sql = "SELECT substr(min(a.ayear || a.sms), 1, 3) as ayear, substr(min(a.ayear || a.sms), 4, 1) as sms  " +
						"FROM grat003 a " +
						"WHERE a.num_of_times = (SELECT MAX (num_of_times) " +
                        "FROM grat003 q " +
                        "WHERE q.stno = a.stno) - " + requestMap.get("TIMES") + " " +
						"AND a.app_grad_type in (select app_grad_type from grat003 where num_of_times = a.num_of_times and stno = a.stno) " +
						"AND a.stno = '" + stno + "' ";
			}
			//System.out.println(sql);
			rs	=	GRAT004DAO.query(sql);

			if (rs.next()) {
				rtnValue = rs.getString("ayear") + rs.getString("sms");
			}
			return rtnValue;
		} catch(Exception e) {
			throw e;
		} finally {
			if (rs != null)
				rs.close();
		}
	}

	public void decideGroup(String ayearsms, int[] CRD) throws Exception {
		//System.out.println("ayearsms"+ayearsms);
		String type = rtnType(ayearsms);

		CRD[12] = Integer.parseInt(type)*10 + 1;
		CRD[27] = Integer.parseInt(type)*10 + 2;
		CRD[41] = Integer.parseInt(type)*10 + 3;
		CRD[55] = Integer.parseInt(type)*10 + 4;
		CRD[69] = Integer.parseInt(type)*10 + 5;
		CRD[83] = Integer.parseInt(type)*10 + 6;

		CRD[25] = getPassCrd(type + "1");
		CRD[40] = getPassCrd(type + "2");
		CRD[54] = getPassCrd(type + "3");
		CRD[68] = getPassCrd(type + "4");
		CRD[82] = getPassCrd(type + "5");
		CRD[96] = getPassCrd(type + "6");
	}

	public String rtnType(String ayearsms) throws Exception {
		String type = "";
		if(Integer.parseInt(ayearsms) < 870) {
			type = "1";
		} else if(Integer.parseInt(ayearsms) < 920) {
			type = "2";
		} else if(Integer.parseInt(ayearsms) < 930) {
			type = "3";
		} else {
			type = "4";
		}
		//System.out.println("rtnType="+type);
		return type;
	}

	public String getGroupName(int i) throws Exception {
		String[] groupName = new String[47];
		for(int k = 0 ; k < groupName.length ; k++) {
			groupName[k] = "&nbsp;";
		}
		groupName[11] = "基礎課程";
		groupName[12] = "通識課程";
		groupName[13] = "外國科目";
		groupName[16] = "選修";

		groupName[21] = "國文類";
		groupName[22] = "外文類";
		groupName[23] = "本國歷史類";
		groupName[24] = "憲政類";
		groupName[25] = "通識課程類";
		groupName[26] = "選修";

		groupName[31] = "國文類";
		groupName[32] = "外文類";
		groupName[33] = "通識課程類";
		groupName[36] = "選修";

		groupName[41] = "國文類";
		groupName[42] = "外文類";
		groupName[43] = "通識課程類";
		groupName[46] = "選修";

		return groupName[i];
	}

	public String getGroupCrsno(int lineCode) throws Exception {
		String sql = "select crsno from grat027 where 1 = 1 ";
		String[] groupName = new String[47];
		for(int k = 0 ; k < groupName.length ; k++) {
			groupName[k] = "";
		}

		int digitOne = lineCode / 10;
		int digitTwo = lineCode % 10;

		if (digitTwo >= 1 && digitTwo < 6) {
			sql += "and kind = '" + digitOne + "' and group_code = '" + digitTwo + "' ";
		} else if (digitTwo == 6) {
			sql = "select a.crsno from cout002 a where a.crsno not in (select a.crsno from grat027 a where a.kind = '"
					+ digitOne + "') ";
		} else {
			sql += "and 1 = 2";
		}
		return sql;
	}

	public void DoReduceCRD(DBManager dbManager, Connection conn, String AYEAR, String SMS, String stno, int[] CRD) throws Exception {
		CRD[181] = getReduceCRD(dbManager, conn, AYEAR, SMS, stno);
	}

	public void DoReduceCRD(DBManager dbManager, Connection conn, String AYEAR, String SMS, String stno, int[] CRD, Map map) throws Exception {
		CRD[181] = Integer.parseInt((String) map.get(stno) == null || "".equals(map.get(stno)) ? "0" : (String) map.get(stno));
	}

	public int getReduceCRD(DBManager dbManager, Connection conn, String AYEAR, String SMS, String stno) throws Exception {
		DBResult rs = null;
		int rtnValue = 0;
		try {
			GRAT004DAO GRAT004DAO = new GRAT004DAO(dbManager, conn);
			StringBuffer sql = new StringBuffer();
			sql.append("select m.reduce_crd_total as total ");
			sql.append("from ( ");
			sql.append("SELECT stno,sum(reduce_crd) as reduce_crd_total "); //學號曾經申請畢業未通過之減修學分數
			sql.append("FROM ccst003 ");
			sql.append("WHERE stno = '" + stno + "' ");
			sql.append("AND ayear || DECODE (sms, '1', '2', '2', '3', '3', '1') <= ");
			sql.append("(SELECT ayear || DECODE (sms, '1', '2', '2', '3', '3', '1') ");
			sql.append("FROM grat003 ");
			sql.append("WHERE stno = '" + stno + "' ");
			sql.append("AND num_of_times = ");
			sql.append("(SELECT num_of_times ");
			sql.append("FROM grat003 ");
			sql.append("WHERE ayear = '" + AYEAR + "' AND sms = '" + SMS + "' ");
			sql.append(" AND stno = '" + stno + "') ");
			sql.append("AND app_grad_type IN ('01', '02', '05') AND GRAD_REEXAM_STATUS <> '3') ");
			sql.append("AND ayear || DECODE (sms, '1', '2', '2', '3', '3', '1') > ");
			sql.append("DECODE ((SELECT num_of_times ");
			sql.append("FROM grat003 ");
			sql.append("WHERE ayear = '" + AYEAR + "' AND sms = '1' AND stno = '" + stno + "'), ");
			sql.append("'1', 0, ");
			sql.append("(SELECT ayear || DECODE (sms, '1', '2', '2', '3', '3', '1') ");
			sql.append("FROM grat003 ");
			sql.append("WHERE stno = '" + stno + "' ");
			sql.append("AND num_of_times = ");
			sql.append(" (SELECT num_of_times - 1 ");
			sql.append(" FROM grat003 ");
			sql.append(" WHERE ayear = '" + AYEAR + "' ");
			sql.append(" AND sms = '" + SMS + "' ");
			sql.append(" AND stno = '" + stno + "') ");
			sql.append("AND app_grad_type IN ('01', '02', '05') AND GRAD_REEXAM_STATUS <> '3'))");
			sql.append("group by stno ");
			sql.append("union ");  //學號未曾申請畢業所有減修學分數
			sql.append("SELECT stno,sum(reduce_crd) as reduce_crd_total FROM ccst003 ");
			sql.append("where stno = '" + stno + "' ");
			sql.append("group by stno ");
			sql.append(") m ");
			sql.append("where m.stno = '" + stno + "' ");
			sql.append("and m.reduce_crd_total is not null ");

			rs = GRAT004DAO.query(sql.toString());
			if (rs.next()) {
				rtnValue = rs.getInt("total");
			} else {
				rtnValue = 0;
			}
			return rtnValue;
		} catch (Exception e) {
			throw e;
		} finally {
			if (rs != null)
				rs.close();
		}
	}

	public String getReduceCRDStrOnly(String AYEAR, String SMS, String stno) throws Exception {
		StringBuffer sql = new StringBuffer();
		sql.append("SELECT sum(reduce_crd) as total_181 ");
		sql.append("FROM ccst003 ");
		sql.append("WHERE stno = '" + stno + "' ");
		sql.append("AND ayear || DECODE (sms, '1', '2', '2', '3', '3', '1') <= ");
		sql.append("(SELECT ayear || DECODE (sms, '1', '2', '2', '3', '3', '1') ");
		sql.append("FROM grat003 ");
		sql.append("WHERE stno = '" + stno + "' ");
		sql.append("AND num_of_times = ");
		sql.append("(SELECT num_of_times ");
		sql.append("FROM grat003 ");
		sql.append("WHERE ayear = '" + AYEAR + "' AND sms = '" + SMS + "' ");
		sql.append(" AND stno = '" + stno + "') ");
		sql.append("AND app_grad_type IN ('01', '02', '05') AND GRAD_REEXAM_STATUS <> '3') ");
		sql.append("AND ayear || DECODE (sms, '1', '2', '2', '3', '3', '1') > ");
		sql.append("DECODE ((SELECT num_of_times ");
		sql.append("FROM grat003 ");
		sql.append("WHERE ayear = '" + AYEAR + "' AND sms = '1' AND stno = '" + stno + "'), ");
		sql.append("'1', 0, ");
		sql.append("(SELECT ayear || DECODE (sms, '1', '2', '2', '3', '3', '1') ");
		sql.append("FROM grat003 ");
		sql.append("WHERE stno = '" + stno + "' ");
		sql.append("AND num_of_times = ");
		sql.append(" (SELECT num_of_times - 1 ");
		sql.append(" FROM grat003 ");
		sql.append(" WHERE ayear = '" + AYEAR + "' ");
		sql.append(" AND sms = '" + SMS + "' ");
		sql.append(" AND stno = '" + stno + "') ");
		sql.append("AND app_grad_type IN ('01', '02', '05') AND GRAD_REEXAM_STATUS <> '3'))");

		return sql.toString();
	}

	private int getPassCrd(String lineCode) throws Exception {
		int[] passCrd = new int[47];
		for (int k = 0; k < passCrd.length; k++) {
			passCrd[k] = 0;
		}
		passCrd[11] = 9;
		passCrd[12] = 6;
		passCrd[13] = 6;
		passCrd[16] = 0;

		passCrd[21] = 6;
		passCrd[22] = 6;
		passCrd[23] = 2;
		passCrd[24] = 2;
		passCrd[25] = 12;
		passCrd[26] = 0;

		passCrd[31] = 3;
		passCrd[32] = 3;
		passCrd[33] = 4;
		passCrd[36] = 0;

		passCrd[41] = 3;
		passCrd[42] = 3;
		passCrd[43] = 4;
		passCrd[46] = 0;
		return passCrd[Integer.parseInt(lineCode)];
	}

	public String passOrNot(int i) throws Exception {
		String flag = "";
		if (i == 999) {
			flag = "◎";
		} else if (i != 0) {
			flag = "X";
		} else {
			flag = "&nbsp;";
		}
		return flag;
	}

	public void check90Pass(int[] CRD, String mainAyearSms) throws Exception {
		String type = rtnType(mainAyearSms);
		// 判斷是否有符合該修的共同課程學分 如果符合填寫999 前端印出
		if (type.equals("1")) {
			if (CRD[24] >= CRD[25]) {
				CRD[25] = 999;
			}
			if (CRD[39] >= CRD[40]) {
				CRD[40] = 999;
			}
			if (CRD[53] >= CRD[54]) {
				CRD[54] = 999;
			}

		} else if (type.equals("2")) {
			if (CRD[24] >= CRD[25]) {
				CRD[25] = 999;
			}
			if (CRD[39] >= CRD[40]) {
				CRD[40] = 999;
			}
			if (CRD[53] >= CRD[54]) {
				CRD[54] = 999;
			}
			if (CRD[67] >= CRD[68]) {
				CRD[68] = 999;
			}
			if (CRD[81] >= CRD[82]) {
				CRD[82] = 999;
			}
		} else if (type.equals("3")) {
			if (CRD[24] >= CRD[25]) {
				CRD[25] = 999;
			}
			if (CRD[39] >= CRD[40]) {
				CRD[40] = 999;
			}
			if (CRD[53] >= CRD[54]) {
				CRD[54] = 999;
			}
		} else if (type.equals("4")) {
			if (CRD[24] + CRD[39] + CRD[53] >= CRD[25] + CRD[40] + CRD[54]) {
				CRD[25] = 999;
				CRD[40] = 999;
				CRD[54] = 999;
			}
		}
	}

	public void checkFacultyPass(String DB1, String DB2, int[] CRD) {
		if ((DB1.equals("10") || DB2.equals("10"))) {
			if (CRD[108] >= 75) {
				CRD[109] = 999;
			} else {
				CRD[109] = 888;
			}
		}
		if ((DB1.equals("20") || DB2.equals("20"))) {
			if (CRD[121] >= 75) {
				CRD[122] = 999;
			} else {
				CRD[122] = 888;
			}
		}
		if ((DB1.equals("30") || DB2.equals("30"))) {
			if (CRD[135] >= 75) {
				CRD[136] = 999;
			} else {
				CRD[136] = 888;
			}
		}
		if ((DB1.equals("40") || DB2.equals("40"))) {
			if (CRD[148] >= 75) {
				CRD[149] = 999;
			} else {
				CRD[149] = 888;
			}
		}
		if ((DB1.equals("50") || DB2.equals("50"))) {
			if (CRD[161] >= 75) {
				CRD[162] = 999;
			} else {
				CRD[162] = 888;
			}
		}
		if ((DB1.equals("60") || DB2.equals("60"))) {
			if (CRD[174] >= 75) {
				CRD[175] = 999;
			} else {
				CRD[175] = 888;
			}
		}
	}

	/**
	 * 2008.3.28志祥新增
	 */
	public int FacultyMajorTemp(DBManager dbManager, Connection conn, String ayear, String sms, String stno, String faculty_code, int lineCode) throws Exception {
		DBResult	rs	=	null;
		int rtnValue = 0;
		try {
			GRAT004DAO	GRAT004DAO	=	new GRAT004DAO(dbManager, conn);
			StringBuffer sql = new StringBuffer();
			sql.append("SELECT SUM(A.CRD) AS TOTAL ");
			sql.append("FROM STUT010 A ");
			sql.append("JOIN COUT103 B ON A.AYEAR = B.AYEAR AND A.SMS = B.SMS AND A.CRSNO = B.CRSNO AND A.GET_MANNER != '2'");
			sql.append("WHERE A.STNO = '"+stno+"' AND B.TOTAL_CRS_NO = '01' AND B.CRS_GROUP_CODE = '002' AND B.FACULTY_CODE = '"+faculty_code+"' AND A.SMS != '3' ");
			sql.append("AND A.CRSNO NOT IN ( ");
			sql.append("  SELECT A.CRSNO ");
			sql.append("  FROM STUT010 A ");
			sql.append("  JOIN COUT103 B ON A.AYEAR = B.AYEAR AND A.SMS = B.SMS AND A.CRSNO = B.CRSNO ");
			sql.append("  WHERE A.STNO = '"+stno+"' AND B.TOTAL_CRS_NO = '01' AND B.CRS_GROUP_CODE = '002' AND B.FACULTY_CODE = '90' AND A.SMS != '3' ");
			sql.append(") ");
			StringBuffer sql2 = new StringBuffer();
			sql2.append("SELECT  '" + ht.get("AYEAR") + "' ayear, '" + ht.get("SMS") + "' as sms, stno, '" + ht.get("STTYPE") + "' as kind, decode('" + faculty_code + "', '90', '" + lineCode%10 + "', '0') group_code, decode('" + faculty_code + "', '90', '0', '10', '1', '20', '2', '30', '3', '40', '4', '50', '5', '60', '6', '') faculty_code, a.crsno, a.ayear get_ayear, a.sms get_sms, crd, '' get_manner, '' is_valid, '' upd_user_id, '' upd_date, '' upd_time, '' upd_mk, '' rowstamp ");
			sql2.append("FROM STUT010 A ");
			sql2.append("JOIN COUT103 B ON A.AYEAR = B.AYEAR AND A.SMS = B.SMS AND A.CRSNO = B.CRSNO AND A.GET_MANNER != '2'");
			sql2.append("WHERE A.STNO = '"+stno+"' AND B.TOTAL_CRS_NO = '01' AND B.CRS_GROUP_CODE = '002' AND B.FACULTY_CODE = '"+faculty_code+"' AND A.SMS != '3' ");
			sql2.append("AND A.CRSNO NOT IN ( ");
			sql2.append("  SELECT A.CRSNO ");
			sql2.append("  FROM STUT010 A ");
			sql2.append("  JOIN COUT103 B ON A.AYEAR = B.AYEAR AND A.SMS = B.SMS AND A.CRSNO = B.CRSNO ");
			sql2.append("  WHERE A.STNO = '"+stno+"' AND B.TOTAL_CRS_NO = '01' AND B.CRS_GROUP_CODE = '002' AND B.FACULTY_CODE = '90' AND A.SMS != '3' ");
			sql2.append(") ");

			if("2".equals(ht.get("STTYPE")) || ht.get("STTYPE").equals("3") || ht.get("FLAG").equals("Y")) {
				sql.append("AND A.CRSNO NOT IN (SELECT A.CRSNO FROM GRAT004 A WHERE A.STNO = '"+stno+"' AND A.IS_SUMMER = 'N') ");
				sql2.append("AND A.CRSNO NOT IN (SELECT A.CRSNO FROM GRAT004 A WHERE A.STNO = '"+stno+"' AND A.IS_SUMMER = 'N') ");
			}
			sql.append("GROUP BY A.STNO ");

			rs = GRAT004DAO.query(sql.toString());

			try {
				if(ht.get("stnoOrNot").equals("true")) {
					GRAT004DAO.execute("insert into grat033 (" + sql2.toString() + ")");
				}
			} catch(Exception e) {
				//System.out.println(e);
			}

			//System.out.println("FacultyMajorTemp:"+sql.toString());
			if (rs.next()) {
				rtnValue = rs.getInt("TOTAL");
			}
			return rtnValue;
		} catch(Exception e) {
			throw e;
		} finally {
			if (rs != null)
				rs.close();
		}
	}

	/**
	 *
	 * @param ayear
	 * @param sms
	 * @param stno
	 * @param faculty_code
	 * @param lineCode
	 * @param index
	 * @return
	 * @throws Exception
	 */
	public String FacultyMajorTempStrOnly(String ayear, String sms, String stno, String faculty_code, int lineCode) throws Exception {
		StringBuffer sql = new StringBuffer();
		sql.append("SELECT SUM(A.CRD) AS TOTAL ");
		sql.append("FROM STUT010 A ");
		sql.append("JOIN COUT103 B ON A.AYEAR = B.AYEAR AND A.SMS = B.SMS AND A.CRSNO = B.CRSNO AND A.GET_MANNER != '2'");
		sql.append("WHERE A.STNO = '" + stno + "' AND B.TOTAL_CRS_NO = '01' AND B.CRS_GROUP_CODE = '002' AND B.FACULTY_CODE = '" + faculty_code
				+ "' AND A.SMS != '3' ");
		sql.append("AND A.CRSNO NOT IN ( ");
		sql.append("  SELECT A.CRSNO ");
		sql.append("  FROM STUT010 A ");
		sql.append("  JOIN COUT103 B ON A.AYEAR = B.AYEAR AND A.SMS = B.SMS AND A.CRSNO = B.CRSNO ");
		sql.append("  WHERE A.STNO = '" + stno + "' AND B.TOTAL_CRS_NO = '01' AND B.CRS_GROUP_CODE = '002' AND B.FACULTY_CODE = '90' AND A.SMS != '3' ");
		sql.append(") ");

		if ("2".equals(ht.get("STTYPE")) || ht.get("STTYPE").equals("3") || ht.get("FLAG").equals("Y")) {
			sql.append("AND A.CRSNO NOT IN (SELECT A.CRSNO FROM GRAT004 A WHERE A.STNO = '" + stno + "' AND A.IS_SUMMER = 'N') ");
		}
		sql.append("GROUP BY A.STNO ");

		return sql.toString();
	}

	/**
	 * 批次列印時取得學生的來源
	 *
	 * @param dbManager
	 * @param conn
	 * @param requestMap
	 * @return
	 * @throws Exception
	 */
    public DBResult getGradStudents(DBManager dbManager, Connection conn, Hashtable requestMap) throws Exception {
		DBResult rs = null;
		StringBuffer sql = new StringBuffer();
		try {
			GRAT003DAO GRAT003DAO = new GRAT003DAO(dbManager, conn);

			// 空大生批次篩選過後, 由於學生尚未登錄畢業申請資料, 故要從Grat009抓學生資料
			if (requestMap.get("TIMES").equals("0") && requestMap.get("STTYPE").equals("1")) {
				sql.append("select stno, '0' as decrease from grat009 where 1 = 1 ");
				sql.append("and ayear = '" + requestMap.get("AYEAR") + "' and sms = '" + requestMap.get("SMS") + "' ");
				sql.append(((!requestMap.get("STNO").equals("")) ? "and stno = '" + requestMap.get("STNO") + "'" : ""));
			} else {
				sql.append("select a.stno, b.decrease from (select stno, max(num_of_times) max_num from grat003 group by stno) a inner join ( ");
				sql.append("select distinct a.stno, decode(enroll_status, '2', '0', '5', '1', '5') as decrease, a.ayear, a.sms, a.num_of_times from grat003 a ");
				sql.append("inner join stut003 b on a.stno = b.stno where 1 = 1 and grad_reexam_status <> '3'  ");
				sql.append(((!requestMap.get("AYEAR").equals("") && requestMap.get("TIMES").equals("0")) ? "and a.ayear = '" + requestMap.get("AYEAR") + "' " : ""));
				sql.append(((!requestMap.get("SMS").equals("") && requestMap.get("TIMES").equals("0")) ? "and a.sms = '" + requestMap.get("SMS") + "' " : ""));
				sql.append(((!requestMap.get("STNO").equals("")) ? "and a.stno = '" + requestMap.get("STNO") + "'" : ""));
				sql.append(((requestMap.get("STTYPE").equals("2")) ? "and a.app_grad_type in ('01', '02') " : ""));
				sql.append(((requestMap.get("STTYPE").equals("3")) ? "and a.app_grad_type in ('04', '05') " : ""));
				sql.append(" ) b on a.stno = b.stno and a.max_num - " + requestMap.get("TIMES") + " + b.decrease = b.num_of_times ");
			}
			
			GRAT003DAO.execute("delete from grat033");
			return GRAT003DAO.query(sql.toString());
		} catch (Exception e) {
			throw e;
		} finally {
			if (rs != null){
				rs.close();
			}

			dbManager.commit();
		}
	}

    public void appendLog(DBManager dbManager, Connection conn, Hashtable requestMap, RptFile rptFile) throws Exception {
		DBResult rs = null;
		String content = "<center><table align=center border=0 width=700 cellspacing=0 cellpadding=0>";
		if (true) {
			try {
				GRAT003DAO GRAT003DAO = new GRAT003DAO(dbManager, conn);
				StringBuffer sql = new StringBuffer();
				sql.append("select a.stno, a.group_code, a.faculty_code, a.crsno, ");
				sql.append("a.get_ayear || a.get_sms as GETAYEARSMS, a.crd, b.crs_name ");
				sql.append("from grat033 a ");
				sql.append("inner join cout002 b on a.crsno = b.crsno ");
				sql.append("order by a.stno, a.group_code, a.faculty_code, a.crsno");
				rs = GRAT003DAO.query(sql.toString());

				while (rs.next()) {
					content += "<tr><td width=100 align=left>" + rs.getString("STNO") + "</td><td width=75 align=left>grow= " + rs.getString("GROUP_CODE")
							+ "</td><td width=75 align=left>drow= " + rs.getString("FACULTY_CODE") + "</td><td width=150 align=left>" + rs.getString("CRSNO")
							+ "-" + rs.getString("GETAYEARSMS") + "- " + rs.getString("CRD") + "</td><td align=left width=300>" + rs.getString("CRS_NAME")
							+ "</td></tr>";
				}
				content += "</table></center>";
			} catch (Exception e) {
				throw e;
			} finally {
				if (rs != null)
					rs.close();

			}
		}

		rptFile.add(content);
	}

    public void appendLogForAudit(DBManager dbManager, Connection conn, Hashtable requestMap, RptFile rptFile) throws Exception {
		String ayear = (String) requestMap.get("AYEAR");
		String sms = (String) requestMap.get("SMS");
		String stno = (String) requestMap.get("STNO");
		String use_type = (String) requestMap.get("USE_TYPE");
		String kind = "1";
		if("3".equals(use_type)){
			kind = "2";
		}else if("2".equals(use_type)){
			kind = "1";	
		}
		DBResult rs = null;
		StringBuffer str = new StringBuffer();
		str.append("<center>                                                      \n");
		str.append("<table width='720' border='0' cellspacing='0' cellpadding='2'>\n");
		str.append("<tr align='center'>                                           \n");
		str.append("    <td width='80'>　</td>                                    \n");
		str.append("    <td width='90'>　</td>                                    \n");
		str.append("    <td width='105'>　</td>                                   \n");
		str.append("    <td width='30'>　</td>                                    \n");
		str.append("    <td width='30'>　</td>                                    \n");
		str.append("    <td width='200'>　</td>                                   \n");
		str.append("    <td width='117'>　</td>                                   \n");
		str.append("</tr>                                                         \n");

		try {
			GRAT003DAO GRAT003DAO = new GRAT003DAO(dbManager, conn);
			StringBuffer sql = new StringBuffer();
			sql.append("select a.stno, a.group_code, a.faculty_code, a.crsno, b.crs_name, a.get_ayear, a.get_sms, a.crd,  ");
			sql.append("DECODE(GET_MANNER,'1','修','2','抵','3','推','4','當','5', '暑') AS GET_MANNER, a.is_valid ");
			sql.append("from grat030 a ");
			sql.append("join cout002 b on a.crsno = b.crsno ");
			sql.append("where a.stno = '" + stno + "' and a.ayear = '" + ayear + "' and a.sms = '" + sms + "' and a.kind = '"+kind+"' ");
			sql.append("order by a.group_code, a.faculty_code, a.crsno ");
			rs = GRAT003DAO.query(sql.toString());

			String fCode = null;
			int crd = 0;
			int index = 0;
			while (rs.next()) {
				String IS_VALID = rs.getString("IS_VALID");
				String color = "#000000";
				String valid_name = "";
				if ("N".equals(IS_VALID)) {
					color = "#FF0000";
					valid_name = "無效科目";
				} else if ("B".equals(IS_VALID)) {
					color = "#0000FF";
					valid_name = "多科取N科";
				} else if ("G".equals(IS_VALID)) {
					color = "#00FF00";
					valid_name = "多科取N學分，<font color=red>請手動確認〝總學分〞</font>";
				}

				if (!rs.getString("FACULTY_CODE").equals(fCode) && index != 0) {
					str.append("<tr align='left' style='color:#000000'>                       \n");
					str.append("    <td></td>                                                 \n");
					str.append("    <td></td>                                                 \n");
					str.append("    <td>合計學分</td>                                          \n");
					str.append("    <td>" + crd + "</td> 									  \n");
					str.append("    <td></td>                                                 \n");
					str.append("    <td></td>                                                 \n");
					str.append("    <td></td>                                                 \n");
					str.append("</tr>                                                         \n");
					str.append("<tr align='left' style='color:#000000'>                       \n");
					str.append("    <td></td>                                                 \n");
					str.append("    <td></td>                                                 \n");
					str.append("    <td></td>                                                 \n");
					str.append("    <td></td>                                                 \n");
					str.append("    <td></td>                                                 \n");
					str.append("    <td></td>                                                 \n");
					str.append("    <td></td>                                                 \n");
					str.append("</tr>                                                         \n");

					crd = 0;
				}

				str.append("<tr align='left' style='color:" + color + "'>                       \n");
				str.append("    <td>" + stno + "</td>                                        \n");
				str.append("    <td>dep= " + rs.getString("FACULTY_CODE") + "</td>                                          \n");
				str.append("    <td>" + rs.getString("CRSNO") + "-" + rs.getString("GET_AYEAR") + rs.getString("GET_SMS") + "</td>   \n");
				str.append("    <td>" + rs.getString("CRD") + "</td>                                                \n");
				str.append("    <td>" + rs.getString("GET_MANNER") + "</td>                                               \n");
				str.append("    <td>" + rs.getString("CRS_NAME") + "</td>                                             \n");
				str.append("    <td>" + valid_name+"</td>                                                 \n");
				str.append("</tr>                                                         \n");

				fCode = rs.getString("FACULTY_CODE");
				crd += Integer.parseInt(rs.getString("CRD"));
				index++;
			}

			str.append("<tr align='left' style='color:#000000'>                       \n");
			str.append("    <td></td>                                                 \n");			
			str.append("    <td></td>                                                 \n");
			str.append("    <td>合計學分</td>                                          \n");
			str.append("    <td>" + crd + "</td>                                      \n");
			str.append("    <td></td>                                                 \n");
			str.append("    <td></td>                                                 \n");
			str.append("    <td></td>                                                 \n");
			str.append("</tr>                                                         \n");
			str.append("<tr align='left' style='color:#000000'>                       \n");
			str.append("    <td></td>                                                 \n");
			str.append("    <td></td>                                                 \n");
			str.append("    <td></td>                                                 \n");
			str.append("    <td></td>                                                 \n");
			str.append("    <td></td>                                                 \n");
			str.append("    <td></td>                                                 \n");
			str.append("    <td></td>                                                 \n");
			str.append("</tr>                                                         \n");
			str.append("</table>                                                      \n");
			str.append("</center>                                                     \n");
		} catch (Exception e) {
			throw e;
		} finally {
			if (rs != null) {
				rs.close();
			}
		}

		rptFile.add(str.toString());
	}

    /**
	 * 以下為新程式使用 取得單主修與新制雙主修的每一格的學分數
	 *
	 * @param dbManager
	 * @param conn
	 * @param requestMap
	 * @return
	 * @throws Exception
	 */
	public Map getCrdForAuditOfKind0102(DBManager dbManager, Connection conn, Hashtable requestMap, String auditKind) throws Exception {		
		String ayear = (String) requestMap.get("AYEAR");
		String sms = (String) requestMap.get("SMS");
		String stno = (String) requestMap.get("STNO");
		String gel_type = Utility.checkNull(requestMap.get("GEL_TYPE"),"");
        String PRINT_RANGE1 = (String) requestMap.get("PRINT_RANGE1");
		String PRINT_RANGE2 = (String) requestMap.get("PRINT_RANGE2");
		String FACULTY_CODE = Utility.checkNull(requestMap.get("FACULTY_CODE"),"");
		String CENTER_CODE = Utility.checkNull(requestMap.get("CENTER_CODE"),"");
		String kind = Utility.checkNull(requestMap.get("GEL_PRINT_TYPE"),"");
		
		Hashtable ht = getGradType(dbManager,conn,requestMap);
		StringBuffer sql = new StringBuffer();
		//by poto 2009/05/18
        //處理畢業多科取N學分
		requestMap.put("KIND","1");//kind = 1 初審
		String getCrdSql = getCrdSql(requestMap);
		sql.append("select a.stno, a.app_grad_type, a.sttype, a.sttype_name, a.grad_cen, a.dep1, a.dep2, a.accum_reduce_crd, a.name, a.grade_score,a.ayear, a.sms,a.sms_name, \n");
		sql.append("a.enroll_ayearsms, a.center_abbrname, b.total, b.faculty_code, b.get_manner, b.is_adopt, nvl(b.group_code, 0) as group_code, \n");
		sql.append("a.dep_code1, a.dep_code2 ");
		sql.append(getCrdSql);    //by poto 2009/05/18   處理畢業多科取N學分
		sql.append("from (  \n");
		sql.append(CrdAdoptSql.getStudentInfoForKind01(ayear, sms));// 取得該學年期的單主修畢業生的資訊
		sql.append(" union \n");
		sql.append(CrdAdoptSql.getStudentInfoForKind02(ayear, sms));// 取得該學年期的新制雙主修畢業生的資訊
		sql.append(") a join ( ");
		sql.append(CrdAdoptSql.getGeneralCrdOfNonCommon(ayear, sms,auditKind));// 取得上下學年期實得學分數非共同科(kind1,2,5共用)
		sql.append(" union \n");
		sql.append(CrdAdoptSql.getGeneralCrdOfCommon(ayear, sms,kind, auditKind));// 取得上下學年期實得學分數共同科(kind1,2,5共用)
		sql.append(" union \n");
		sql.append(CrdAdoptSql.getThisYearCrdOfNonCommonForKind0102(ayear, sms));// 取得當學年期非共同科學系開設(單主修與新制雙主修)
		sql.append(" union \n");
		sql.append(CrdAdoptSql.getThisYearCrdOfCommonForKind0102(ayear, sms,kind));// 取得當學年期共同科學系開設(單主修與新制雙主修)
		sql.append(" union \n");
		sql.append(CrdAdoptSql.getThisYearAdoptCrdOfNonCommon(ayear, sms));// 取得當學年期非共同科申請歸併(kind1,2,5共用)
		sql.append(" union \n");
		sql.append(CrdAdoptSql.getThisYearAdoptCrdOfCommon(ayear, sms, kind));// 取得當學年期共同科申請歸併(kind1,2,5共用)
		sql.append(" union \n");
		sql.append(CrdAdoptSql.getSummerCrdOfNonCommonForKind0102(ayear, sms,ht));// 取得暑期實得學分非共同科學系開設(單主修與新制雙主修)
		sql.append(" union \n");
		sql.append(CrdAdoptSql.getSummerCrdOfCommonForKind0102(ayear, sms,ht,kind));// 取得暑期實得學分共同科學系開設(單主修與新制雙主修)
		sql.append(" union \n");
		sql.append(CrdAdoptSql.getSummerAdoptCrdOfNonCommonForKind0102(ayear, sms,ht));// 取得暑期實得學分非共同科申請歸併(單主修與新制雙主修)
		sql.append(" union \n");
		sql.append(CrdAdoptSql.getSummerAdoptCrdOfCommonForKind0102(ayear, sms,ht,kind));// 取得暑期實得學分共同科申請歸併(單主修與新制雙主修)
		sql.append(") b on a.stno = b.stno                               \n");
        if (stno != null && !"".equals(stno)) {
			sql.append(" where a.stno in ('" + stno + "') ");
		}else if(!"".equals(FACULTY_CODE)||!"".equals(CENTER_CODE)||(PRINT_RANGE1!=null&&PRINT_RANGE2!=null&&!"".equals(PRINT_RANGE1)&&!"".equals(PRINT_RANGE2))){
		    requestMap.put("TABLE_NAME","GRAT003");
           	sql.append(" where a.stno = (" + getSql(dbManager,conn,requestMap)+" AND a.stno =B.STNO " + ") ");
        }
        sql.append("    order by grad_cen,decode(a.app_grad_type,'05',a.dep_code2,a.dep_code1),stno, faculty_code, get_manner, is_adopt\n");        
		DBResult rs = null;
		Map map = new LinkedHashMap();
		try {
			rs = dbManager.getSimpleResultSet(conn);
			rs.open();
			System.out.println(sql.toString());
			rs.executeQuery(sql.toString());

			CrdAdoptPrinter cap = null;
			List sList = null;
			while (rs.next()) {
				if (map.get(rs.getString("stno")) == null) {
					cap = new CrdAdoptPrinter();
					cap.setStno(rs.getString("stno"));
					cap.setSttype(rs.getString("sttype"));
					cap.setSttypeName(rs.getString("sttype_name"));
					cap.setGradCen(rs.getString("grad_cen"));
					cap.setDep1(rs.getString("dep1"));
					cap.setDep2(rs.getString("dep2"));
					cap.setAccumReduceCrd(rs.getString("accum_reduce_crd"));
					cap.setName(rs.getString("name"));
					cap.setEnrollAyearsms(rs.getString("enroll_ayearsms"));
					cap.setCenterAbbrname(rs.getString("center_abbrname"));
					cap.setDepCode1(rs.getString("dep_code1"));
					cap.setDepCode2(rs.getString("dep_code2"));
					cap.setAddcrd(rs.getString("addcrd"));
					//by pot 加上 畢業成績 和 畢業學年期
					cap.setGradeScore(rs.getString("grade_score"));
					cap.setAyear(rs.getString("ayear"));
					cap.setSms(rs.getString("sms"));
					cap.setSmsName(rs.getString("sms_name"));
					//by poto 加上 kind
					cap.setApp_grad_type(rs.getString("app_grad_type"));

					sList = new ArrayList();
					CrdAdoptCrd cac = new CrdAdoptCrd();
					cac.setTotal(rs.getString("total"));
					cac.setFacultyCode(rs.getString("faculty_code"));
					cac.setGetManner(rs.getString("get_manner"));
					cac.setIsAdopt(rs.getString("is_adopt"));
					cac.setGroupCode(rs.getString("group_code"));

					sList.add(cac);
					cap.setList(sList);
					map.put(rs.getString("stno"), cap);
				} else {
					cap = (CrdAdoptPrinter) map.get(rs.getString("stno"));
					sList = cap.getList();

					CrdAdoptCrd cac = new CrdAdoptCrd();
					cac.setTotal(rs.getString("total"));
					cac.setFacultyCode(rs.getString("faculty_code"));
					cac.setGetManner(rs.getString("get_manner"));
					cac.setIsAdopt(rs.getString("is_adopt"));
					cac.setGroupCode(rs.getString("group_code"));

					sList.add(cac);
					cap.setList(sList);
					map.put(rs.getString("stno"), cap);
				}
			}
		} catch (Exception e) {
			throw e;
		} finally {
			if (rs != null) {
				rs.close();
			}
		}

		return map;
	}

	/**
	 * 取得舊制雙主修的每一格的學分數
	 *
	 * @param dbManager
	 * @param conn
	 * @param requestMap
	 * @return
	 * @throws Exception
	 */
	public Map getCrdForAuditOfKind05(DBManager dbManager, Connection conn, Hashtable requestMap, String auditKind) throws Exception {
		String ayear = (String) requestMap.get("AYEAR");
		String sms = (String) requestMap.get("SMS");
		String stno = (String) requestMap.get("STNO");

		List list = null;
		// 單筆
		if (stno != null && !"".equals(stno)) {
			list = new ArrayList();
			list.add(stno);
		} else {// 多筆
			list = this.getAppGradType05(dbManager, conn, requestMap);
		}

		List kList = new LinkedList();
		StringBuffer sql = new StringBuffer();
		if (list != null && list.size() > 0) {
			for (int i = 0; i < list.size(); i++) {
				String stnos = (String) list.get(i);
				String kind = this.getKindForKind05(dbManager, conn, requestMap, stnos);
				kList.add(kind);
                System.out.println("stno ="+stnos);
				if (i != 0) {
					sql.append(" union \n");
				}

				sql.append("select a.stno, a.sttype, a.sttype_name, a.grad_cen, a.dep1, a.dep2, a.accum_reduce_crd, a.name,    \n");
				sql.append("a.enroll_ayearsms, a.center_abbrname, a.grade_score, a.ayear, a.sms, a.sms_name, a.dep_code1,      \n");
				//by poto 抓 kind = 5的畢業成績 跟畢業學年其
				sql.append("a.grade_score1,a.ayear1,a.sms1,a.sms_name1, \n");
				sql.append("a.dep_code2, b.total, b.faculty_code, b.get_manner, b.is_adopt, nvl(b.group_code, 0) as group_code, \n");
				sql.append("a.app_grad_type \n");
				sql.append("from (                                                                                             \n");
				sql.append(CrdAdoptSql.getStudentInfoForKind05(ayear, sms));// 取得該學年期的舊制雙主修畢業生的資訊
				sql.append(") a join ( ");
				sql.append(CrdAdoptSql.getGeneralCrdOfNonCommon(ayear, sms,auditKind));// 取得上下學年期實得學分數非共同科(kind1,2,5共用)
				sql.append(" union \n");
				sql.append(CrdAdoptSql.getGeneralCrdOfCommon(ayear, sms,kind, auditKind));// 取得上下學年期實得學分數共同科(kind1,2,5共用)
				sql.append(" union \n");
				sql.append(CrdAdoptSql.getThisYearCrdOfNonCommonForKind05(ayear, sms));// 取得當學年期非共同科學系開設(舊制雙主修)
				sql.append(" union \n");
				sql.append(CrdAdoptSql.getThisYearCrdOfCommonForKind05(ayear, sms, kind));// 取得當學年期共同科學系開設(舊制雙主修)
				sql.append(" union \n");
				sql.append(CrdAdoptSql.getThisYearAdoptCrdOfNonCommon(ayear, sms));// 取得當學年期非共同科申請歸併(kind1,2,5共用)
				sql.append(" union \n");
				sql.append(CrdAdoptSql.getThisYearAdoptCrdOfCommon(ayear, sms, kind));// 取得當學年期共同科申請歸併(kind1,2,5共用)
				sql.append(" union \n");
				sql.append(CrdAdoptSql.getSummerCrdOfNonCommonForKind05(ayear, sms));// 取得暑期實得學分非共同科學系開設(舊制雙主修)
				sql.append(" union \n");
				sql.append(CrdAdoptSql.getSummerCrdOfCommonForKind05(ayear, sms, kind));// 取得暑期實得學分共同科學系開設(舊制雙主修)
				sql.append(" union \n");
				sql.append(CrdAdoptSql.getSummerAdoptCrdOfNonCommonForKind05(ayear, sms));// 取得暑期實得學分非共同科申請歸併(舊制雙主修)
				sql.append(" union \n");
				sql.append(CrdAdoptSql.getSummerAdoptCrdOfCommonForKind05(ayear, sms, kind));// 取得暑期實得學分共同科申請歸併(舊制雙主修)
				sql.append("    order by stno, faculty_code, get_manner, is_adopt\n");
				sql.append(") b on a.stno = b.stno                               \n");
				sql.append(" where a.stno = '" + stnos + "' and a.center_code like '"+requestMap.get("CENTER_CODE")+"%' ");
			}
		}

		DBResult rs = null;
		Map map = new LinkedHashMap();

		if ("".equals(sql.toString())) {
			return map;
		}

		try {
			rs = dbManager.getSimpleResultSet(conn);
			rs.open();
			//System.out.println(sql.toString());
			rs.executeQuery(sql.toString());

			CrdAdoptPrinter cap = null;
			List sList = null;
			int index = 0;
			while (rs.next()) {
				if (map.get(rs.getString("stno")) == null) {
					cap = new CrdAdoptPrinter();
					cap.setStno(rs.getString("stno"));
					cap.setSttype(rs.getString("sttype"));
					cap.setSttypeName(rs.getString("sttype_name"));
					cap.setGradCen(rs.getString("grad_cen"));
					cap.setDep1(rs.getString("dep1"));
					cap.setDep2(rs.getString("dep2"));
					cap.setAccumReduceCrd(rs.getString("accum_reduce_crd"));
					cap.setName(rs.getString("name"));
					cap.setEnrollAyearsms(rs.getString("enroll_ayearsms"));
					cap.setCenterAbbrname(rs.getString("center_abbrname"));
					cap.setDepCode1(rs.getString("dep_code1"));
					cap.setDepCode2(rs.getString("dep_code2"));
					cap.setGradeScore(rs.getString("grade_score"));
					cap.setSmsName(rs.getString("sms_name"));
					//by poto  2009/03/17
                    //新增畢業後再修學系總平均
					System.out.println("auditKind"+auditKind);
					if("2".equals(auditKind)){
						cap.setAyear1(rs.getString("ayear1"));
						cap.setSms1(rs.getString("sms1"));
						cap.setSmsName1(rs.getString("sms_name1"));
					}
					cap.setGradeScoreSecond(rs.getString("grade_score1"));					
					cap.setAyear(rs.getString("ayear"));
					cap.setSms(rs.getString("sms"));
					cap.setAppGradType(rs.getString("app_grad_type"));
					cap.setKind((String)kList.get(index));

					sList = new LinkedList();
					CrdAdoptCrd cac = new CrdAdoptCrd();
					cac.setTotal(rs.getString("total"));
					cac.setFacultyCode(rs.getString("faculty_code"));
					cac.setGetManner(rs.getString("get_manner"));
					cac.setIsAdopt(rs.getString("is_adopt"));
					cac.setGroupCode(rs.getString("group_code"));

					sList.add(cac);
					cap.setList(sList);
					map.put(rs.getString("stno"), cap);

					index++;
				} else {
					cap = (CrdAdoptPrinter) map.get(rs.getString("stno"));
					sList = cap.getList();

					CrdAdoptCrd cac = new CrdAdoptCrd();
					cac.setTotal(rs.getString("total"));
					cac.setFacultyCode(rs.getString("faculty_code"));
					cac.setGetManner(rs.getString("get_manner"));
					cac.setIsAdopt(rs.getString("is_adopt"));
					cac.setGroupCode(rs.getString("group_code"));

					sList.add(cac);
					cap.setList(sList);
					map.put(rs.getString("stno"), cap);
				}
			}
		} catch (Exception e) {
			throw e;
		} finally {
			if (rs != null) {
				rs.close();
			}
		}

		return map;
	}

	/**
	 * 取得舊制雙主修的每一格的學分數(申請用)
	 *
	 * @param dbManager
	 * @param conn
	 * @param requestMap
	 * @return
	 * @throws Exception
	 */
	public Map getCrdForApplyOfKind05(DBManager dbManager, Connection conn, Hashtable requestMap) throws Exception {
		String ayear = (String) requestMap.get("AYEAR");
		String sms = (String) requestMap.get("SMS");
		String stno = (String) requestMap.get("STNO");
		int check = 1;
		List list = null;
		// 單筆
		if (stno != null && !"".equals(stno)) {			
			list = new ArrayList();
			list.add(stno);
		} else {// 多筆
			list = this.getAppGradType05ForApply(dbManager, conn, requestMap);
		}

		List kList = new ArrayList();
		StringBuffer sql = new StringBuffer();
		if (list != null && list.size() > 0) {
			for (int i = 0; i < list.size(); i++) {
				String stnos = (String) list.get(i);
				//by poto 20090721 為了 沒篩選也可以印
				//1 存在 0 不存在
				
		        if(stnos != null && !"".equals(stnos)) {
		    		requestMap.put("TABLE","GRAT009");
		            check = getCheckTable(dbManager,conn, requestMap);
		        } 
				
				String kind = this.getApplyKindForKind05(dbManager, conn, requestMap, stnos);
				kList.add(kind);

				if (i != 0) {
					sql.append(" union \n");
				}

				sql.append("select a.stno, a.sttype, a.sttype_name, a.grad_cen, a.dep1, a.dep2, a.accum_reduce_crd, a.name,    \n");
				sql.append("a.enroll_ayearsms, a.center_abbrname, a.grade_score, a.ayear, a.sms, a.sms_name, a.dep_code1,      \n");
				sql.append("a.dep_code2, b.total, b.faculty_code, b.get_manner, b.is_adopt, nvl(b.group_code, 0) as group_code, \n");
				sql.append("a.app_grad_type \n");
				sql.append("from (                                                                                             \n");
				sql.append(CrdAdoptSql.getApplyStudentInfoForKind05(ayear, sms));// 取得該學年期的舊制雙主修畢業生的資訊
				sql.append(") a join ( ");
				sql.append(CrdAdoptSql.getApply01(ayear, sms));// 取得實得學分非共同科(舊制雙主修)
				sql.append(" union \n");
				sql.append(CrdAdoptSql.getApply02(ayear, sms, kind));// 取得實得學分共同科(舊制雙主修)
				sql.append(" union \n");
				sql.append(CrdAdoptSql.getApply03(ayear));// 取得實得學分非共同科申請歸併(舊制雙主修)
				sql.append(" union \n");
				sql.append(CrdAdoptSql.getApply04(ayear,kind));// 取得實得學分共同科申請歸併(舊制雙主修)
				sql.append(" union \n");
				sql.append(CrdAdoptSql.getApply05(ayear, sms));// 取得當學期非共同科(舊制雙主修)
				sql.append(" union \n");
				sql.append(CrdAdoptSql.getApply06(ayear, sms, kind));// 取得當學期共同科(舊制雙主修)
				sql.append(" union \n");
				sql.append(CrdAdoptSql.getApply07(ayear, sms));// 取得當學期非共同科申請歸併(舊制雙主修)
				sql.append(" union \n");
				sql.append(CrdAdoptSql.getApply08(ayear, sms, kind));// 取得當學期共同科申請歸併(舊制雙主修)
				sql.append(" union \n");
				sql.append(CrdAdoptSql.getApply09(ayear, sms));// 取得暑期非共同科(舊制雙主修)
				sql.append(" union \n");
				sql.append(CrdAdoptSql.getApply10(ayear, sms, kind));// 取得暑期共同科(舊制雙主修)
				sql.append(" union \n");
				sql.append(CrdAdoptSql.getApply11(ayear));// 取得暑期非共同科申請歸併(舊制雙主修)
				sql.append(" union \n");
				sql.append(CrdAdoptSql.getApply12(ayear,kind));// 取得暑期共同科申請歸併(舊制雙主修)
				sql.append(" union \n");
				sql.append(CrdAdoptSql.getApply13(ayear));// 取得暑期採計非共同科(舊制雙主修)
				sql.append(" union \n");
				sql.append(CrdAdoptSql.getApply14(ayear,kind));// 取得暑期採計共同科(舊制雙主修)
				sql.append(" union \n");
				sql.append(CrdAdoptSql.getApply15(ayear));// 取得暑期採計非共同科申請歸併(舊制雙主修)
				sql.append(" union \n");
				sql.append(CrdAdoptSql.getApply16(ayear,kind));// 取得暑期採計共同科申請歸併(舊制雙主修)
				sql.append(" union \n");
				sql.append(CrdAdoptSql.getApply17(ayear, sms));// 取得抵免非共同科(舊制雙主修)
				sql.append(" union \n");
				sql.append(CrdAdoptSql.getApply18(ayear, sms, kind));// 取得抵免共同科(舊制雙主修)
				sql.append(" union \n");
				sql.append(CrdAdoptSql.getApply19(ayear, sms));// 取得抵免非共同科申請歸併(舊制雙主修)
				sql.append(" union \n");
				sql.append(CrdAdoptSql.getApply20(ayear, sms, kind));// 取得抵免共同科申請歸併(舊制雙主修)
				//sql.append("    order by stno, faculty_code, get_manner, is_adopt\n");
				sql.append(") b on a.stno = b.stno                               \n");
				sql.append("where a.stno = '" + stnos + "' and a.center_code like '"+requestMap.get("CENTER_CODE")+"%' order by grad_cen,stno, faculty_code, get_manner, is_adopt \n");
			}
		}

		DBResult rs = null;
		Map map = new LinkedHashMap();

		if ("".equals(sql.toString())) {
			return map;
		}

		try {
			rs = dbManager.getSimpleResultSet(conn);
			rs.open();
			//System.out.println(sql.toString());
			rs.executeQuery(sql.toString());

			CrdAdoptPrinter cap = null;
			List sList = null;
			int index = 0;
			while (rs.next()) {
				if (map.get(rs.getString("stno")) == null) {
					cap = new CrdAdoptPrinter();
					cap.setStno(rs.getString("stno"));
					cap.setSttype(rs.getString("sttype"));
					cap.setSttypeName(rs.getString("sttype_name"));
					cap.setGradCen(rs.getString("grad_cen"));
					cap.setDep1(rs.getString("dep1"));
					cap.setDep2(rs.getString("dep2"));
					cap.setAccumReduceCrd(rs.getString("accum_reduce_crd"));
					cap.setName(rs.getString("name"));
					cap.setEnrollAyearsms(rs.getString("enroll_ayearsms"));
					cap.setCenterAbbrname(rs.getString("center_abbrname"));
					cap.setDepCode1(rs.getString("dep_code1"));
					cap.setDepCode2(rs.getString("dep_code2"));
					cap.setGradeScore(rs.getString("grade_score"));
					cap.setAyear(rs.getString("ayear"));
					cap.setSms(rs.getString("sms"));
					cap.setSmsName(rs.getString("sms_name"));
					cap.setAppGradType(rs.getString("app_grad_type"));
					cap.setKind((String) kList.get(index));

					sList = new ArrayList();
					CrdAdoptCrd cac = new CrdAdoptCrd();
					cac.setTotal(rs.getString("total"));
					cac.setFacultyCode(rs.getString("faculty_code"));
					cac.setGetManner(rs.getString("get_manner"));
					cac.setIsAdopt(rs.getString("is_adopt"));
					cac.setGroupCode(rs.getString("group_code"));

					sList.add(cac);
					cap.setList(sList);
					map.put(rs.getString("stno"), cap);

					index++;
				} else {
					cap = (CrdAdoptPrinter) map.get(rs.getString("stno"));
					sList = cap.getList();

					CrdAdoptCrd cac = new CrdAdoptCrd();
					cac.setTotal(rs.getString("total"));
					cac.setFacultyCode(rs.getString("faculty_code"));
					cac.setGetManner(rs.getString("get_manner"));
					cac.setIsAdopt(rs.getString("is_adopt"));
					cac.setGroupCode(rs.getString("group_code"));

					sList.add(cac);
					cap.setList(sList);
					map.put(rs.getString("stno"), cap);
				}
			}
		} catch (Exception e) {
			throw e;
		} finally {
			if(check==0){
                getDelTable(dbManager,conn, requestMap);
            }
			if (rs != null) {
				rs.close();
			}
		}

		return map;
	}

	/**
	 * 取得舊制雙主修的共同科的種類
	 *
	 * @param dbManager
	 * @param conn
	 * @param requestMap
	 * @return
	 * @throws Exception
	 */
	public String getKindForKind05(DBManager dbManager, Connection conn, Hashtable requestMap, String stno) throws Exception {
		String ayear = (String) requestMap.get("AYEAR");
		String sms = (String) requestMap.get("SMS");

		StringBuffer sql = new StringBuffer();

		sql.append("select                                                                               \n");
		sql.append("case when to_number(x.ayear) < 87 then 1                                             \n");
		sql.append("when to_number(x.ayear) >= 87 and to_number(x.ayear) <= 91 then 2                    \n");
		sql.append("when to_number(x.ayear) >= 92 and to_number(x.ayear) <= 93 then 3                    \n");
		sql.append("else 4 end as kind                                                                   \n");
		sql.append(" from grat003 x where x.app_grad_type = '04' and x.num_of_times = (                  \n");
		sql.append("select a.num_of_times from grat003 a                                                 \n");
		sql.append("where a.ayear = '" + ayear + "' and a.sms = '" + sms + "' and a.app_grad_type = '05' and x.stno = a.stno \n");
		sql.append(") and x.grad_reexam_status = '2' and x.stno = '" + stno + "'                            \n");

		DBResult rs = null;
		try {
			rs = dbManager.getSimpleResultSet(conn);
			rs.open();
			//System.out.println(sql.toString());
			rs.executeQuery(sql.toString());

			if (rs.next()) {
				return getGelkind( (String) requestMap.get("AYEAR"),rs.getString("kind"));
			}
		} catch (Exception e) {
			throw e;
		} finally {
			if (rs != null) {
				rs.close();
			}
		}

		return "4";
	}

	/**
	 * 取得舊制雙主修的共同科的種類(申請用)
	 *
	 * @param dbManager
	 * @param conn
	 * @param requestMap
	 * @param stno
	 * @return
	 * @throws Exception
	 */
	public String getApplyKindForKind05(DBManager dbManager, Connection conn, Hashtable requestMap, String stno) throws Exception {
		StringBuffer sql = new StringBuffer();

		sql.append("select                                                            \n");
		sql.append("case when to_number(x.ayear) < 87 then 1                          \n");
		sql.append("when to_number(x.ayear) >= 87 and to_number(x.ayear) <= 91 then 2 \n");
		sql.append("when to_number(x.ayear) >= 92 and to_number(x.ayear) <= 93 then 3 \n");
		sql.append("else 4 end as kind                                                \n");
		sql.append("from grat003 x                                                    \n");
		sql.append("where x.app_grad_type = '04'                                      \n");
		sql.append("and x.grad_reexam_status = '2' and x.stno = '" + stno + "'           \n");
		sql.append("and x.num_of_times = (                                            \n");
		sql.append("    select max(a.num_of_times) from grat003 a                     \n");
		sql.append("    where x.grad_reexam_status = a.grad_reexam_status             \n");
		sql.append("    and a.app_grad_type = x.app_grad_type and x.stno = a.stno     \n");
		sql.append(")                                                                 \n");

		DBResult rs = null;
		try {
			rs = dbManager.getSimpleResultSet(conn);
			rs.open();
			//System.out.println(sql.toString());
			rs.executeQuery(sql.toString());

			if (rs.next()) {
				return getGelkind( (String) requestMap.get("AYEAR"),rs.getString("kind"));
			}
		} catch (Exception e) {
			throw e;
		} finally {
			if (rs != null) {
				rs.close();
			}
		}

		return "4";
	}

	/**
	 * 取得該學年期的所有舊制雙主修的申請資料
	 *
	 * @param dbManager
	 * @param conn
	 * @param requestMap
	 * @return
	 * @throws Exception
	 */
	public List getAppGradType05(DBManager dbManager, Connection conn, Hashtable requestMap) throws Exception {
		String ayear = (String) requestMap.get("AYEAR");
		String sms = (String) requestMap.get("SMS");

		StringBuffer sql = new StringBuffer();

		sql.append("select a.stno from grat003 a where a.ayear = '" + ayear + "' and a.sms = '" + sms + "' and a.grad_cen like '"+requestMap.get("CENTER_CODE")+"%' and a.app_grad_type = '05' ORDER BY a.GRAD_CEN ,NVL(a.GRAD_MAJOR_FACULTY,a.DBMAJOR_GRAD_FACULTY_CODE1),a.STNO \n");

		DBResult rs = null;
		List list = new LinkedList();
		try {
			rs = dbManager.getSimpleResultSet(conn);
			rs.open();
			rs.executeQuery(sql.toString());

			while (rs.next()) {
				list.add(rs.getString("stno"));
			}
		} catch (Exception e) {
			throw e;
		} finally {
			if (rs != null) {
				rs.close();
			}
		}

		return list;
	}

	/**
	 * 取得該學年期的所有舊制雙主修的篩選資料(申請用)
	 *
	 * @param dbManager
	 * @param conn
	 * @param requestMap
	 * @return
	 * @throws Exception
	 */
	public List getAppGradType05ForApply(DBManager dbManager, Connection conn, Hashtable requestMap) throws Exception {
		String ayear = (String) requestMap.get("AYEAR");
		String sms = (String) requestMap.get("SMS");
		String stno = (String) requestMap.get("STNO");

		StringBuffer sql = new StringBuffer();
        sql.append("select a.stno " +
        		   "from grat009 a " +
        		   "where a.ayear = '" + ayear + "' and a.sms = '" + sms + "' " +
        		   "and a.center_code like '"+requestMap.get("CENTER_CODE")+"%' and a.is_time_after_time = 'Y' "+
        		   "");
        if(!"".equals(stno)&&stno!=null){
             sql.append(" and a.stno = '"+stno+"' " );
        }
        sql.append(" order by a.center_code, a.stno \n");

		//sql.append("select a.stno from grat009 a where a.ayear = '" + ayear + "' and a.sms = '" + sms + "' and a.is_time_after_time = 'Y' \n");

		DBResult rs = null;
		List list = new ArrayList();
		try {
			rs = dbManager.getSimpleResultSet(conn);
			rs.open();
			rs.executeQuery(sql.toString());

			while (rs.next()) {
				list.add(rs.getString("stno"));
			}
		} catch (Exception e) {
			throw e;
		} finally {
			if (rs != null) {
				rs.close();
			}
		}
		return list;
	}

	/**
	 * 取得已修已抵的學生(kind01,02,05共用)
	 *
	 * @param dbManager
	 * @param conn
	 * @param requestMap
	 * @return
	 * @throws Exception
	 */
	public List 取得已修已抵的學生(DBManager dbManager, Connection conn, Hashtable requestMap, String kind) throws Exception {
		String ayear = (String) requestMap.get("AYEAR");
		String sms = (String) requestMap.get("SMS");
		
		String type = "'01', '02'";
		if ("05".equals(kind)) {
			type = "'05'";
		}

		StringBuffer sql = new StringBuffer();

		sql.append("select a.stno                                                                                      \n");
		sql.append("from grat003 a                                                                                     \n");
		sql.append("join (                                                                                             \n");
		sql.append("    select distinct stno from scdt004 where trim(pass_repl_mk) is not null and crsno_smsgpa >= '60'\n");
		sql.append("    union                                                                                          \n");
		sql.append("    select distinct stno from ccst003 where trim(pass_repl_mk) is not null                         \n");
		sql.append(") b on a.stno=b.stno                                                                               \n");
		sql.append("join stut003 c on a.stno = c.stno 																   \n");
		sql.append("where a.grad_reexam_status = '2' and a.ayear = '" + ayear + "' and a.sms = '" + sms + "'		   \n");
		sql.append("and a.grad_cen like '"+requestMap.get("CENTER_CODE")+"%' 										   \n");
		sql.append("and a.app_grad_type in (" + type + ")                                                              \n");
        sql.append("order by a.grad_cen ,NVL(a.GRAD_MAJOR_FACULTY,a.DBMAJOR_GRAD_FACULTY_CODE1),a.STNO                 \n");
		DBResult rs = null;
		List list = new ArrayList();
		try {
			rs = dbManager.getSimpleResultSet(conn);
			rs.open();
			rs.executeQuery(sql.toString());

			while (rs.next()) {
				list.add(rs.getString("stno"));
			}
		} catch (Exception e) {
			throw e;
		} finally {
			if (rs != null) {
				rs.close();
			}
		}

		return list;
	}

	/**
	 * 申請用(單主修與新制雙主修)
	 * @param dbManager
	 * @param conn
	 * @param requestMap
	 * @param auditKind
	 * @return
	 * @throws Exception
	 */
	public Map getCrdForApplyOfKind0102(DBManager dbManager, Connection conn, Hashtable requestMap) throws Exception {
		String ayear = (String) requestMap.get("AYEAR");
		String sms = (String) requestMap.get("SMS");
		String stno = (String) requestMap.get("STNO");
		String gel_type = Utility.nullToSpace(requestMap.get("GEL_TYPE"));
		String PRINT_RANGE1 = (String) requestMap.get("PRINT_RANGE1");
		String PRINT_RANGE2 = (String) requestMap.get("PRINT_RANGE2");
		String FACULTY_CODE = (String) requestMap.get("FACULTY_CODE");
		String CENTER_CODE = (String) requestMap.get("CENTER_CODE");
		String kind = Utility.checkNull(requestMap.get("GEL_PRINT_TYPE"),"");
		
        Hashtable ht = getGradType(dbManager,conn,requestMap);
        //by poto 20090721 為了 沒篩選也可以印
		//1 存在 0 不存在
		int check = 1;
        if(stno != null && !"".equals(stno)) {
    		requestMap.put("TABLE","GRAT009");
            check = getCheckTable(dbManager,conn, requestMap);
        }
		StringBuffer sql = new StringBuffer();
		sql.append("select a.stno, a.sttype, a.sttype_name, a.center_code as grad_cen, a.dep1, a.dep2, a.accum_reduce_crd, a.name,       \n");
		sql.append("a.enroll_ayearsms, a.center_abbrname, b.total, b.faculty_code, b.get_manner, b.is_adopt, nvl(b.group_code, 0) as group_code, \n");
		sql.append("a.dep_code1, a.dep_code2 ");
		sql.append("from (  \n");
        sql.append(CrdAdoptSql.getApplyStudentInfoForKind01(ayear, sms));// 取得該學年期的畢業生的資訊
		sql.append(") a join ( ");
		sql.append(CrdAdoptSql.getApplyGeneralCrdOfNonCommon(ayear, sms,ht));// 實得學分非共同科(單主修與新制雙主修)
		sql.append(" union \n");
		sql.append(CrdAdoptSql.getApplyGeneralCrdOfCommon(ayear, sms,ht,kind));// 取得實得學分共同科(單主修與新制雙主修)
		sql.append(" union \n");
		sql.append(CrdAdoptSql.getApplyThisYearCrdOfNonCommonForKind0102(ayear, sms));// 取得當學期非共同科(單主修與新制雙主修)
		sql.append(" union \n");
		sql.append(CrdAdoptSql.getApplyThisYearCrdOfCommonForKind0102(ayear, sms,kind));// 取得當學期共同科(單主修與新制雙主修)
		sql.append(" union \n");
		sql.append(CrdAdoptSql.getApplySummerCrdOfNonCommonForKind0102(ayear, sms,ht));// 取得暑期非共同科(單主修與新制雙主修)
		sql.append(" union \n");
		sql.append(CrdAdoptSql.getApplySummerCrdOfCommonForKind0102(ayear, sms,ht,kind));// 取得暑期共同科(單主修與新制雙主修)
		sql.append(" union \n");
		sql.append(CrdAdoptSql.getApplyReplCrdOfNonCommonForKind0102(ayear, sms,ht));// 取得抵免非共同科(單主修與新制雙主修)
		sql.append(" union \n");
		sql.append(CrdAdoptSql.getApplyReplCrdOfCommonForKind0102(ayear, sms,ht,kind));// 取得抵免共同科(單主修與新制雙主修)
		//sql.append("    order by stno, faculty_code, get_manner, is_adopt\n");
		sql.append(") b on a.stno = b.stno                             \n");
		
		if (stno != null && !"".equals(stno)) {
			sql.append(" where a.stno in ('" + stno + "') ");
		}else if(!"".equals(FACULTY_CODE)||!"".equals(CENTER_CODE)||(PRINT_RANGE1!=null&&PRINT_RANGE2!=null&&!"".equals(PRINT_RANGE1)&&!"".equals(PRINT_RANGE2))){
		    requestMap.put("TABLE_NAME","GRAT009");
           	sql.append(" where a.stno = (" + getSql(dbManager,conn,requestMap)+" AND a.stno =B.STNO " + ") ");
        }
		sql.append("order by grad_cen,stno, faculty_code, get_manner, is_adopt   \n");
		DBResult rs = null;
		Map map = new LinkedHashMap();
		try {
			rs = dbManager.getSimpleResultSet(conn);
			rs.open();
			System.out.println(sql.toString());
			rs.executeQuery(sql.toString());

			CrdAdoptPrinter cap = null;
			List sList = null;
			while (rs.next()) {
				if (map.get(rs.getString("stno")) == null) {
				    System.out.println("stno="+rs.getString("stno"));
					cap = new CrdAdoptPrinter();
					cap.setStno(rs.getString("stno"));
					cap.setSttype(rs.getString("sttype"));
					cap.setSttypeName(rs.getString("sttype_name"));
					cap.setGradCen(rs.getString("grad_cen"));
					cap.setDep1(rs.getString("dep1"));
					cap.setDep2(rs.getString("dep2"));
					cap.setAccumReduceCrd(rs.getString("accum_reduce_crd"));
					cap.setName(rs.getString("name"));
					cap.setEnrollAyearsms(rs.getString("enroll_ayearsms"));
					cap.setCenterAbbrname(rs.getString("center_abbrname"));
					cap.setDepCode1(rs.getString("dep_code1"));
					cap.setDepCode2(rs.getString("dep_code2"));

					sList = new LinkedList();
					CrdAdoptCrd cac = new CrdAdoptCrd();
					cac.setTotal(rs.getString("total"));
					cac.setFacultyCode(rs.getString("faculty_code"));
					cac.setGetManner(rs.getString("get_manner"));
					cac.setIsAdopt(rs.getString("is_adopt"));
					cac.setGroupCode(rs.getString("group_code"));

					sList.add(cac);
					cap.setList(sList);
					map.put(rs.getString("stno"), cap);
				} else {
					cap = (CrdAdoptPrinter) map.get(rs.getString("stno"));
					sList = cap.getList();

					CrdAdoptCrd cac = new CrdAdoptCrd();
					cac.setTotal(rs.getString("total"));
					cac.setFacultyCode(rs.getString("faculty_code"));
					cac.setGetManner(rs.getString("get_manner"));
					cac.setIsAdopt(rs.getString("is_adopt"));
					cac.setGroupCode(rs.getString("group_code"));

					sList.add(cac);
					cap.setList(sList);
					map.put(rs.getString("stno"), cap);
				}
			}
		} catch (Exception e) {
			throw e;
		} finally {
		    if(check==0){
                getDelTable(dbManager,conn, requestMap);
            }
			if (rs != null) {
				rs.close();
			}
		}

		return map;
	}


	/**
	 * 取得該當世資料by poto
	 *
	 * @param dbManager
	 * @param conn
	 * @param requestMap
	 * @return
	 * @throws Exception
	 */
	public Hashtable getGradType(DBManager dbManager, Connection conn, Hashtable requestMap) throws Exception {

		String stno = (String) requestMap.get("STNO");
		Hashtable ht = new Hashtable();
		StringBuffer sql = new StringBuffer();
        sql.append("select \n");
        sql.append("g3.AYEAR,g3.SMS \n");
        sql.append("from grat003 g3 \n");
        sql.append("where 1=1 \n");
        sql.append("and g3.stno ='"+stno+"' \n");
        sql.append("and g3.GRAD_REEXAM_STATUS ='2' and g3.APP_GRAD_TYPE != '04' \n");
        sql.append("and g3.NUM_OF_TIMES = ( select MAX(NUM_OF_TIMES) from grat003 where g3.stno = stno and GRAD_REEXAM_STATUS ='2' ) \n");
        if(!Utility.nullToSpace(requestMap.get("AYEAR")).equals("")&&!Utility.nullToSpace(requestMap.get("SMS")).equals("")) {
            sql.append("AND g3.AYEAR||g3.SMS != '" + Utility.nullToSpace(requestMap.get("AYEAR")) + Utility.nullToSpace(requestMap.get("SMS")) +"' ");
        }
        sql.append("order by g3.AYEAR||g3.SMS desc \n");

		DBResult rs = null;
		List list = new ArrayList();
		try {
			rs = dbManager.getSimpleResultSet(conn);
			rs.open();
			rs.executeQuery(sql.toString());

			if(rs.next()) {
				ht.put("AYEAR",rs.getString("AYEAR"));
				ht.put("SMS",rs.getString("SMS"));
			}
		} catch (Exception e) {
			throw e;
		} finally {
			if (rs != null) {
				rs.close();
			}
		}
		return ht;
	}

    /**
	 * 取得為曾畢業的人數分段
	 *
	 * @param dbManager
	 * @param conn
	 * @param requestMap
	 * @return
	 * @throws Exception
	 */
	public String getSql(DBManager dbManager, Connection conn, Hashtable requestMap) throws Exception {

		String TABLE_NAME = (String) requestMap.get("TABLE_NAME");
		String AYEAR = (String) requestMap.get("AYEAR");
		String SMS = (String) requestMap.get("SMS");
		String GEL_TYPE = Utility.checkNull(requestMap.get("GEL_TYPE"),"");
		String PRINT_RANGE1 = (String) requestMap.get("PRINT_RANGE1");
		String PRINT_RANGE2 = (String) requestMap.get("PRINT_RANGE2");
		String PRINT_RANGE =   "";
		if(PRINT_RANGE1!=null&&PRINT_RANGE2!=null&&!"".equals(PRINT_RANGE1)&&!"".equals(PRINT_RANGE2)){
			PRINT_RANGE =   "AND ID between  "+PRINT_RANGE1+" AND "+ PRINT_RANGE2;			
		}		
		String FACULTY_CODE = Utility.checkNull(requestMap.get("FACULTY_CODE"),"");
		StringBuffer sql = new StringBuffer();
		if("GRAT009".equals(TABLE_NAME)){
    		sql.append("SELECT STNO FROM ( ");
        	sql.append("SELECT A.* ,rownum AS ID FROM (");
        	sql.append(" select a.stno, a.center_code " +
        			   " from GRAT009 a " +
        			   " join STUT003 b on a.stno = b.stno "+
        			   " where a.ayear = '"+AYEAR+"' and a.sms = '"+SMS+"' and a.is_time_after_time = 'N' and a.center_code like '"+requestMap.get("CENTER_CODE")+"%'  ORDER by a.center_code,a.stno ");
        	sql.append(") A ");
        	sql.append(") B WHERE 1=1 ");
        	sql.append(PRINT_RANGE);
        }else if("GRAT003".equals(TABLE_NAME)){
            sql.append("SELECT STNO FROM ( ");
        	sql.append("SELECT A.* ,rownum AS ID FROM ( ");
        	sql.append(" 	select a.stno,a.GRAD_CEN ");
			sql.append("	from GRAT003 a ");
			sql.append("	join STUT003 b on a.stno = b.stno ");
        	sql.append("    where a.ayear = '"+AYEAR+"' and a.sms = '"+SMS+"' ");
        	sql.append("    and a.APP_GRAD_TYPE != '05' and a.GRAD_CEN like '"+requestMap.get("CENTER_CODE")+"%' ");
        	if( !"".equals(FACULTY_CODE) ){
            	sql.append("AND (TRIM(a.GRAD_MAJOR_FACULTY) ='"+FACULTY_CODE+"' OR TRIM(a.DBMAJOR_GRAD_FACULTY_CODE1) ='"+FACULTY_CODE+"' OR TRIM(a.DBMAJOR_GRAD_FACULTY_CODE2) ='"+FACULTY_CODE+"') ");
        	}
			sql.append("ORDER BY GRAD_CEN ,NVL(GRAD_MAJOR_FACULTY,DBMAJOR_GRAD_FACULTY_CODE1),STNO  ");
        	sql.append(") A ");
        	sql.append(") B WHERE 1=1 ");
        	sql.append(PRINT_RANGE);
        }
		System.out.println(sql.toString());
		return sql.toString();
	}
	/**
	by poto
	2009/05/18
	畢業多科取N學分問題
	grat015 grat002
	*/
    public String getCrdSql(Hashtable requestMap) throws Exception {
		String AYEAR = (String) requestMap.get("AYEAR");
		String SMS = (String) requestMap.get("SMS");
	    String STNO = (String) requestMap.get("STNO");
	    String KIND = (String) requestMap.get("KIND");
		StringBuffer sql = new StringBuffer();
        sql.append("\n");
        sql.append(",nvl((  \n");
        sql.append("select sum(b.crd) - sum(a.crd) AS G_CRD \n");
        sql.append("from grat030 a \n");
        sql.append("join cout002 b on a.crsno = b.crsno \n");
        sql.append("join grat015 c on A.AYEAR = C.AYEAR AND A.SMS = C.SMS AND A.CRSNO = C.CRSNO AND A.FACULTY_CODE = C.FACULTY_CODE \n");
        sql.append("join grat002 d on c.AYEAR = d.AYEAR AND c.SMS = d.SMS AND c.CRSNO_GROUP_ID = d.CRSNO_GROUP_ID AND c.FACULTY_CODE = d.FACULTY_CODE AND D.IS_ADD_CRD_TO_TOTAL = 'Y' \n");
        sql.append("where 1=1 \n");
        sql.append("AND a.stno = '"+STNO+"' \n");
        sql.append("and a.ayear = '"+AYEAR+"' and a.sms = '"+SMS+"' \n");
        sql.append("and a.kind = '"+KIND+"' and a.is_valid = 'G' \n");
        sql.append("),0) AS addcrd \n");
		return sql.toString();
	}

    /**
	20090721
	檢查在不在GRAT003 or GRAT009 裡面
	// 1 存在  0 不存在
	 */
	public int getCheckTable(DBManager dbManager, Connection conn, Hashtable requestMap) throws Exception {
        int count = 0;
		String TABLE_NAME = (String) requestMap.get("TABLE");
		String AYEAR = (String) requestMap.get("AYEAR");
		String SMS = (String) requestMap.get("SMS");
		String STNO = (String) requestMap.get("STNO");
		String STTYPE = (String) requestMap.get("STTYPE");
		if("0".equals(STTYPE)){
            STTYPE = "N";
        }else if("1".equals(STTYPE)){
            STTYPE = "Y";
        }else{
            STTYPE = "N";
        }
		StringBuffer sql = new StringBuffer();
		if("GRAT009".equals(TABLE_NAME)){
    		GRAT009DAO G9 = new GRAT009DAO(dbManager,conn);
    		G9.setResultColumn(" 1 ");
    		G9.setWhere(" AYEAR = '"+AYEAR+"' AND SMS = '"+SMS+"' AND STNO ='"+STNO+"' ");
            DBResult rs =  G9.query();
            if(rs.next()){
                count = 1;
            }else{
                count = 0;
                sql.append("insert into grat009 ( \n");
                sql.append("   AYEAR,SMS,STNO,IDNO,UPD_USER_ID,UPD_DATE,UPD_TIME,UPD_MK,ROWSTAMP,CRD_SUM,\n");
                sql.append("   ASYS,CENTER_CODE,GRAD_YN ,COMMON_SUM,MAJOR_SUM,SUMMER_OR_PRO_SUM,POP_OR_RELATIVE_SUM,is_time_after_time \n");
                sql.append(")( \n");
                sql.append("	select '"+AYEAR+"' AS AYEAR,'"+SMS+"' AS SMS,STNO,IDNO,'TEMP' AS UPD_USER_ID,TO_CHAR(SYSDATE,'YYYYMMDD') AS UPD_DATE,\n");
                sql.append("	TO_CHAR(SYSDATE,'HH24MISS') AS UPD_TIME,'1' AS UPD_MK,TO_CHAR(SYSDATE,'HH24MISSsss') AS ROWSTAMP,\n");
                sql.append("	'0' AS CRD_SUM,'1' AS ASYS,CENTER_CODE AS CENTER_CODE,'N' AS GRAD_YN,'0' AS COMMON_SUM,'0' AS MAJOR_SUM,\n");
                sql.append("	'0' AS SUMMER_OR_PRO_SUM,'0' AS POP_OR_RELATIVE_SUM ,'"+STTYPE+"' AS is_time_after_time \n");
                sql.append("	from stut003 where stno = '"+STNO+"'\n");
                sql.append(") \n");
                G9.execute(sql.toString());
            }
            rs.close();
        }
        return count;
	}

    public int getDelTable(DBManager dbManager, Connection conn, Hashtable requestMap) throws Exception {
        int count = 0;
		String TABLE_NAME = (String) requestMap.get("TABLE");
		String AYEAR = (String) requestMap.get("AYEAR");
		String SMS = (String) requestMap.get("SMS");
		String STNO = (String) requestMap.get("STNO");
		StringBuffer sql = new StringBuffer();
		if("GRAT009".equals(TABLE_NAME)){
    		GRAT009DAO G9 = new GRAT009DAO(dbManager,conn);
    		count = G9.delete(" AYEAR = '"+AYEAR+"' AND SMS = '"+SMS+"' AND STNO ='"+STNO+"' AND UPD_USER_ID = 'TEMP'");
        }
        return count;
	}
    
    /**
	 * 如果是102之後 kind 都要5 不是的話 就照之前的
	 * 5 新
	 * */
	private String getGelkind(String ayear,String kind) throws Exception {
		String s = "";
        try{
        	if( Integer.parseInt(ayear) >= Integer.parseInt(ParameterUtil.SCD_GEL_AYEARSMS_1) ){
            	s = "5";
        	}else{
        		s = kind;
        	}
        }catch(Exception e){

        }
		return s;
	}

}
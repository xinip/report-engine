package com.github.reportengine.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.math.NumberUtils;

import com.github.rapid.common.util.DateConvertUtil;
import com.github.rapid.common.util.ObjectUtil;
import com.github.reportengine.model.Chart;
import com.github.reportengine.model.Chart.Ser;

/**
 * 杂项工具类
 * @author Irwin
 * @warning 如果你看到这个，那么说明你现在已经在负责我以前的项目了。
 * I am so, so sorry for you.Good speed, guy!
 */
public class MiscUtil {
	
	private static final int DAY_SECONDS = 86400 * 1000;

	/**
	 * 计算总耗时/某kpi
	 * @param totalDur 分子
	 * @param kpiValue 分母
	 * @return hh:mm:ss
	 */
	public static String getDuration(Object totalDur, Object kpiValue ) {
		if(ObjectUtil.isEmpty(totalDur) || ObjectUtil.isEmpty(kpiValue ) || Double.valueOf(kpiValue .toString()).longValue()==0) {
			return calDuration(0D);
		}
		double quotient = (double)Double.valueOf(totalDur.toString())/(double)Double.valueOf(kpiValue .toString());
		if (quotient > DAY_SECONDS) {
			return "--:--:--";
		}
		return calDuration(quotient);
	}

//	public static String calDuration(double quotient) {
//		long fTime = Double.valueOf(quotient).longValue();
//		long h=fTime/3600;
//		long m=fTime/60%60; 
//		long s=fTime%60;
//		long ms=fTime%1000;
//		String TIME_FORMAT = "%02d:%02d:%02d.%0" + Long.toString(ms).length() + "d";
//		return String.format(TIME_FORMAT,h,m,s,ms);
//	}
	
	public static String calDuration(Double quotient){
		if (ObjectUtil.isEmpty(quotient)) {
			quotient = 0D;
		}
		StringBuilder sb = new StringBuilder();
		long ms = Double.valueOf(quotient%1000).longValue();
		long s = Double.valueOf(quotient/1000%60).longValue();
		long m = Double.valueOf(quotient/1000/60%60).longValue();
		long h =Double.valueOf(quotient/1000/60/60).longValue();
		sb.append(h).append(":").append(m).append(":").append(s).append(".").append(ms);
		return sb.toString();
	}
	
	/**
	 * 计算百分比，类似:kpi_value(%)
	 * 若分子为空或者分母为空、0的时候，显示空白字符串
	 * @param member
	 * @param denominator
	 * @return
	 */
	public static String parseRateString(Object member, Object denominator) {
		StringBuffer sb = new StringBuffer();
		if (ObjectUtil.isNotEmpty(member) && Double.valueOf(member .toString()).longValue()!=0 && ObjectUtil.isNotEmpty(denominator) && Double.valueOf(denominator .toString()).longValue()!=0) {
			sb.append(member.toString());
			sb.append("(");
			sb.append(ViewUtils.percent(member, denominator));
			sb.append(")");
		}
		return sb.toString();
	}
	
	/**
	 * union all N个数据集
	 * @param dataList1
	 * @param dataList2
	 * @return
	 */
	public static List<Map<String, Object>> unionAll(List<Map<String, Object>> ... dataLists){
		List<Map<String, Object>> resultList = new ArrayList<Map<String,Object>>();
		for (int i = 0; i < dataLists.length; i++) {
			if (dataLists[i].size() != 0) {
				resultList.addAll(dataLists[i]);
			}
		}
		return resultList;
	}
	
	/**
	 * 动态画图，通过直接set sers
	 * @param dataList 查询数据集，必须包含行转列后字段kpis(Map<String,Object>)
	 * @param chart 展示的图标
	 * @param dim 画图维度
	 * @param kpi 指标名称
	 */
	public static void initDynamicChart(List<Map<String, Object>> dataList, Chart chart, String dim, String kpi) {
		final String tdate = "tdate";
		Map<String, List<Number>> dimKpiValues = new LinkedHashMap<String, List<Number>>();
		List<String> tdateList = getTdateList(dataList, tdate);
		for (Map<String, Object> row : dataList) {
			String kpiValue = (String)row.get(dim);
			if (! dimKpiValues.containsKey(kpiValue)) {
				List<Number> values = new LinkedList<Number>();
				for (String date : tdateList) {
					values.add(null);
					for (Map<String, Object> rowInDim : dataList) {
						if (rowInDim.get(dim).equals(kpiValue) && DateConvertUtil.format((Date)rowInDim.get(tdate), "yyyy-MM-dd").equals(date)) {
							values.remove(values.size() - 1);
							Map<String, Number> kpisMap = (Map<String, Number>) rowInDim.get("kpis");
							values.add(kpisMap.get(kpi));
						}
						dimKpiValues.put(kpiValue, values);
					}
				}
				
				chart.addSer(new Ser(kpiValue, values));
			}
		}
	}
	
	public static boolean mapContainkey(Map<String, Object> map, String key){
		return map.containsKey(key);
	}
	
	/**
	 * 根据dataList，chart，title画图
	 */
	public static void initChartOrderFunnelDetial(List<List<Map<String, Object>>> dataLists, Chart chart) {
		//若数据集列表为空，则返回
		if (dataLists.isEmpty()) {
			errorShow(chart,dataLists.size());
			return;
		}
		try {
			//由于数据集某eventid不存在,发生数组溢出Exception,捕获异常,不处理
			List<String> titles = new LinkedList<String>();
			List<Map<String, Object>> lastList = dataLists.get(dataLists.size() - 1);
			Long maxSeq = NumberUtils.toLong(lastList.get(lastList.size() - 1).get("seq").toString());
			
			if (maxSeq <= 1) {
				errorShow(chart,dataLists.size());
				return;
			}
			for (int i = 0; i <= maxSeq - 2; i++) {
				String title = (String)lastList.get(i).get("step_name");
				title = "从 " + title + " 至 " + lastList.get(i+1).get("step_name") + " 日总转化率";
				titles.add(title);
			}
			for (int i = 0; i < titles.size(); i++) {
				List<Number> values = new LinkedList<Number>();
				for (List<Map<String, Object>> dataList : dataLists) {
					
					String change = i< dataList.size() -1 ? dataList.get(i+1).get("event.totalChange").toString().replace("%", "") : "0";
					values.add((Number)Double.valueOf(change));
				}
				chart.addSer(new Ser(titles.get(i), values));
			}
		} catch (Exception e) {
			//返回空展现
			errorShow(chart,dataLists.size());
		}
	}

	private static void errorShow(Chart chart, int size) {
		List<Number> values = new LinkedList<Number>();
		for (int i=0; i<size; i++) {
			values.add(null);
		}
		chart.addSer(new Ser("显示此表明漏斗错误！",values));
		chart.setxTitle("漏斗步骤数据不全，请检查漏斗设置!");
	}
	
	private static List<String> getTdateList(List<Map<String, Object>> dataList, String tdate){
		
		List<Map<String, Object>>  tdateList = distinctByCol(dataList, tdate);
		List<String> dateList = new ArrayList<String>();
		for (Map<String, Object> row : tdateList) {
			dateList.add(DateConvertUtil.format((Date)row.get(tdate),"yyyy-MM-dd"));
		}
		return dateList;
	}
	
	/**
	 * 漏斗数据组织，获取开始/结束步骤
	 * @param dataList
	 * @return
	 */
	public static List<Map<String, Object>> sutitableFunnel(List<Map<String, Object>> dataList) {
		List<Map<String, Object>> resultDataList = new ArrayList<Map<String,Object>>();
		Set<Object> keySet = new HashSet<Object>();
		
		for (int i = 0;i< dataList.size();i++) {
			Object funnelId = dataList.get(i).get("funnel_id");
			if (keySet.contains(funnelId)) {
				continue;
			}
			Integer step = new Integer(1);
			
			for (int j = i+1; j < dataList.size(); j++) {
				Object unfunnelId = dataList.get(j).get("funnel_id");
				Integer nextStep = Integer.valueOf(dataList.get(j).get("steps").toString());
				if (funnelId.equals(unfunnelId) && step.compareTo(nextStep)==0) {
					Map<String, Object> map = new HashMap<String, Object>();
					map.putAll(dataList.get(i));
					map.put("first_step", dataList.get(j).get("last_step"));
					resultDataList.add(map);
				}
			}
			keySet.add(funnelId);
		}
		return resultDataList;
	}
	
	/**
	 * 根据某列从数据集里取出唯一的值
	 * @param dataLis
	 * @param column
	 * @return List<Map<String,Object>>，返回colmn唯一记录的行列表
	 */
	public static List<Map<String, Object>> distinctByCol(List<Map<String, Object>> dataList, String column) {
		List<Map<String, Object>> distinctColDataList = new ArrayList<Map<String,Object>>();
		Set<Map<String, Object>> distinctColDataSet = new HashSet<Map<String,Object>>();
		if (ObjectUtil.isNullOrEmptyString(dataList)) {
			return distinctColDataList;
		}
		for (Map<String, Object> row : dataList) {
			Map<String, Object> unrow = new HashMap<String, Object>();
			unrow.put(column, row.get(column));
			if (distinctColDataSet.add(unrow)) {
				distinctColDataList.add(unrow);
			}
		}
		return distinctColDataList;
	}
	
	public static void urlDecode(List<Map<String, Object>> dataList) throws UnsupportedEncodingException{
		final String key = "content";
		final String enc = "UTF-8";
		for (Map<String, Object> row : dataList) {
			Object value = row.get(key);
			row.put(key, value == null ? null : URLDecoder.decode((String)value, enc));
		}
	}
	
	/**
	 * 设置报表的title
	 * @param chart
	 * @param title
	 * @param value
	 */
	public static void setTitle(Chart chart,String title, String value){
		if (title.equalsIgnoreCase("xTitle")) {
			chart.setxTitle(value);
		}
		if (title.equalsIgnoreCase("yTitle")) {
			chart.setxTitle(value);
		}
	}
}

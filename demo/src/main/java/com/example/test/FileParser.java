package com.example.test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Iterator;

import upps.afa.base.Conf;
import upps.afa.base.Error;
import upps.afa.base.Logger;
import upps.afa.batch.FileParserInterFace;
import cn.com.agree.afa.svc.javaengine.context.JavaDict;
import cn.com.agree.afa.svc.javaengine.context.JavaList;

/**
 * @模块：平台公共库.批量.文件解析
 * @作者： 伍锡强
 * @时间：2017-01-21
 */
public class FileParser {

	private JavaDict runTimeInfo = new JavaDict();
	private FileParserInterFace fileParserInterFaceimpl = null;

	private JavaDict options = new JavaDict();
	private JavaDict fieldTypeMethodMap = new JavaDict();

	private boolean debug = true;
	
	private JavaList deBugInfoList = new JavaList();
	
	private int nowLineIndex;
	private String nowLine;
	private String nowfieldKV;
	
	private int hasReadLine = 0;

	public int getNowLineIndex() {
		return nowLineIndex;
	}
	
	public String getNowLine() {
		return nowLine;
	}

	public String getnowfieldKV() {
		return nowfieldKV;
	}
	

	private void _debuginfo(String msg, Object... args) {
		if (debug) {
			Logger.info("Debug:" + msg, args);
		}
	}

	public void initFieldTypeMethodMap() throws Error {
		try {
			Class<?> clazz = FileParser.class;
			fieldTypeMethodMap.setItem("P", getMethod(clazz, "method_P"));
			fieldTypeMethodMap.setItem("S", getMethod(clazz, "method_S"));
			fieldTypeMethodMap.setItem("N", getMethod(clazz, "method_N"));
			fieldTypeMethodMap.setItem("N0", getMethod(clazz, "method_N0"));
			fieldTypeMethodMap.setItem("A", getMethod(clazz, "method_A"));
			fieldTypeMethodMap.setItem("A0", getMethod(clazz, "method_A0"));
			fieldTypeMethodMap.setItem("AS", getMethod(clazz, "method_AS"));
			fieldTypeMethodMap.setItem("D1", getMethod(clazz, "method_D1"));
			fieldTypeMethodMap.setItem("D2", getMethod(clazz, "method_D2"));
			fieldTypeMethodMap.setItem("T1", getMethod(clazz, "method_T1"));
			fieldTypeMethodMap.setItem("T2", getMethod(clazz, "method_T2"));
		} catch (Exception e) {
			Logger.error("FileParser处理映射方法异常");
			throw Error.SYSTEM_ERROR;
		}
	}

	public Method getMethod(Class<?> clazz, String methodName) throws Error {
		try {
			Class[] paramTypes = new Class[4];
			paramTypes[0] = int.class;
			paramTypes[1] = String.class;
			paramTypes[2] = JavaDict.class;
			paramTypes[3] = JavaDict.class;
			return clazz.getMethod(methodName, paramTypes);
			// return clazz.getMethod(methodName,
			// getMethodParamTypes(clazz,methodName));
		} catch (NoSuchMethodException e) {
			Logger.error("FileParser类找不到映射方法" + methodName);
			throw Error.INPARAMS_INVALID;
		} catch (SecurityException e) {
			Logger.error("FileParser类映射方法异常" + methodName);
			throw Error.SYSTEM_ERROR;
		}
	}

	// public static Class[] getMethodParamTypes(Class<?> clazz,String
	// methodName) throws Error{
	// Class[] paramTypes = null;
	// Method[] methods = clazz.getMethods();
	// for(int i=0; i<methods.length;i++){
	// if(methodName.equals(methods[i].getName())){
	// Class[] params = methods[i].getParameterTypes();
	// paramTypes = new Class[params.length];
	// for (int j = 0; j < params.length; j++) {
	// try {
	// paramTypes[j] = Class.forName(params[j].getName());
	// } catch (ClassNotFoundException e) {
	// Logger.error("FileParser找不到类:"+params[j].getName()==null);
	// throw Error.INPARAMS_INVALID;
	// }
	// }
	// break;
	// }
	// }
	// return paramTypes;
	// }

	public void initOption() {
		options.setItem("encoding", null); // 编码 String
		options.setItem("fileType", 0); // 文件类型，0-定长，1-变长，2-XML(未实现) Integer
		options.setItem("lineBreak", "\n"); // 换行符 String
		options.setItem("separator", ""); // 分隔符 String
		options.setItem("readBeginLineIndex", 0); // 读取开始行号 Integer
		options.setItem("readLineCount", 0); // 读取行数，0-所有 Integer
		options.setItem("readType", 0); // 读取方式，0-逐行读取，1-定长读取 Integer
		options.setItem("fieldTypeMethodMap", null); // 字段类型方法映射 JavaDict 自定义映射
		// 替换平台映射
		JavaDict fieldMustMap = new JavaDict();
		fieldMustMap.setItem("N", 0);//value 0 选填
		fieldMustMap.setItem("Y", 1);//value 1 必填
		fieldMustMap.setItem("false", 0);// value0选填
		fieldMustMap.setItem("true", 1);// value1必填
		fieldMustMap.setItem("O", 0);// value0选填
		fieldMustMap.setItem("M", 1);// value1必填
		fieldMustMap.setItem("NULL", 0);// value0选填
		fieldMustMap.setItem("NOT NULL", 1);// value1必填
		options.setItem("fieldMustMap", fieldMustMap); // 字段必送标识
		options.setItem("allIsOptional", 0); // 全部字段选送 全部字段都可以为空
		options.setItem("fieldDefineWrapper", false); // 字段定义包装器 是否打开 Boolean
		options.setItem("justFlag", "L"); // 对齐标志，L-左对齐（默认），R-右对齐 写入时左对齐右对齐 定长文件
		options.setItem("WriteLineInfoDealMethod", false); // 写入行信息处理器 是否打开  Boolean
		options.setItem("WriteRecordReturnList", false); // 写入后返回给写入行信息处理器的是javaList
														// 还是separator分割的字符串
		options.setItem("reWriteFile", true); // 是否重写文件 true-重写 false追加
		options.setItem("LineBreakreWrite", true); // 追加如果文件不为空则自动写定义的换行符再追加写入	

		options.setItem("lineEndSeparator", false); // 每行最后一个字段是否有分隔符
		options.setItem("lastlineBreak", false); // 每行最后一个字段是否有分隔符
		options.setItem("emptyValueThrowError",false);//检测到空值程序抛出异常还是只打印信息
		options.setItem("debugMode", false); // 是否显示debug信息
	
		options.setItem("pageSize", "1000"); //写出行数缓存
		
		options.setItem("checkLongValue", true); //是否校验长度

		// options.setItem("writeTooLongType", 0); //写入超长类型，0-截断，1-直写，2-报错
		
		// Integer
		// options.setItem("xmlOptions", null); //XML选项
		// options.setItem("checkTotalLen", false); //检查字段总长度 Boolean
		// options.setItem("checkTotalCount", false); //检查字段总数 Boolean
	}

	public void initRunTimeInfo() {
		runTimeInfo.clear();

	}

	/**
	 * 从FileParserInterFace的实现类中导入自定义配置 使用前先初始化FileParser组件
	 * 
	 * @param ClassName
	 *            继承FileParserInterFace的全名（包+类名）
	 * @param platOptions
	 *            传入组件的静态配置option
	 * @param platfieldTypeMethodMethodMap
	 *            传入组件的静态映射platfieldTypeMethodMethodMap
	 * @param runTimeInfo
	 *            传入组件的静态运行信息
	 * @throws Exception
	 */
	public void initFileParser(String ClassName, JavaDict inDict)
			throws Exception {
		initRunTimeInfo();
		if (fileParserInterFaceimpl == null) {
			try {
				fileParserInterFaceimpl = (FileParserInterFace) Class.forName(
						ClassName).newInstance();
			} catch (ClassNotFoundException e) {
				Logger.error("找不到实现类：%s", ClassName);
				throw Error.CLASS_NOT_FOUND;
			}
		}
		fileParserInterFaceimpl.setInDict(inDict);
		fileParserInterFaceimpl.init();
		JavaDict imploptions = fileParserInterFaceimpl.getOptions();
		if (imploptions != null && imploptions.size() > 0) {
			JavaDict implfieldTypeMethodMap = imploptions
					.getDictItem("fieldTypeMethodMap");
			if (implfieldTypeMethodMap != null
					&& implfieldTypeMethodMap.size() > 0) {
				JavaDict FieldTypeMethodMap2 = new JavaDict();
				for (Object key : implfieldTypeMethodMap.keySet()) {
					// 字符串，平台标准
					String fieldType = (String) key;
					Object fieldMethod = implfieldTypeMethodMap
							.getItem(fieldType);
					if (fieldMethod instanceof Method) {
						FieldTypeMethodMap2.setItem(fieldType,
								(Method) fieldMethod);
					} else {
						Logger.error("非法的字段方法:" + fieldType);
						throw Error.INPARAMS_INVALID;
					}
				}
				// 替换平台标准定义
				fieldTypeMethodMap.putAll(FieldTypeMethodMap2);
			}
			// 更新其他选项
			for (Object key : imploptions.keySet()) {
				if (!options.containsKey(key)) {
					Logger.error("无法识别的选项：" + key);
//					throw Error.INPARAMS_INVALID;
				}
			}
			options.putAll(imploptions);
			options.setItem("fieldTypeMethodMap", fieldTypeMethodMap);
			debug = options.getBooleanItem("debugMode");
		}
		// 检测文件体定义
		if (fileParserInterFaceimpl.getBodyDefine() == null) {
			Logger.error("参数不能为空：fileParserInterFaceimpl中bodyDefine为必填选项");
			throw Error.INPARAMS_INVALID;
		}
		// //设置字段信息
		// runTimeInfo.setItem("lineType", 1);
		// _setFieldsInfo();
	}

	/**
	 * 设置字段信息 通过runTimeinfo的lineType通过实现类的Define方法更换当前解析行的读取配置
	 * 
	 * @param JavaDict
	 *            Options 传入组件的静态配置option
	 * @param javaDict
	 *            runTimeInfo 传入组件的静态运行信息
	 * @throws Exception
	 */
	private void _setFieldsInfo() throws Exception {
		JavaList fieldsDefine = new JavaList();
		int Type = runTimeInfo.getIntItem("lineType", -1);
		if (fileParserInterFaceimpl == null) {
			Logger.error(" 初始化失败：没有加载FileParserInterFace实现类");
			throw Error.INPARAMS_INVALID;
		} else {
			if (Type == 0) {
				fieldsDefine = fileParserInterFaceimpl.getHeadDefine();
			} else if (Type == 2) {
				fieldsDefine = fileParserInterFaceimpl.getTailDefine();
			} else if (Type == 1) {
				fieldsDefine = fileParserInterFaceimpl.getBodyDefine();
			} else {
				Logger.error("未知行类型" + Type);
				throw Error.INPARAMS_INVALID;
			}
		}
		// 设置字段信息
		// 字段定义
		runTimeInfo.setItem("fieldsDefine", fieldsDefine);
		// 字段信息
		JavaDict fieldsInfo = new JavaDict();
		JavaList fieldsName = new JavaList();
		String separator = options.getStringItem("separator", null);
		int totalLen = runTimeInfo.getIntItem("totalLen");
		if (fieldsDefine.size() == 0) {
			Logger.error("正在解析字段类型为" + Type + "的长度为0");
			throw Error.INPARAMS_INVALID;
		}

		for (Object fieldDef : fieldsDefine) {
			JavaDict value = new JavaDict();
			// 调用字段定义包装器
			try {
				if (options.getBooleanItem("fieldDefineWrapper") == true) {
					value = fileParserInterFaceimpl
							.fieldDefineWrapper(fieldDef);
				} else {
					value = (JavaDict) fieldDef;
				}
			} catch (Exception e) {
				Logger.error("解析字段信息失败，请检查fieldDefineWrapper配置和fieldDefineWrapper方法");
				throw Error.INPARAMS_INVALID;
			}
			// //扩展信息
			// JavaDict extInfo = null;
			// //XML
			// String fieldTag = null;
			// if(options.getStringItem("fileType") == "2"){
			// fieldTag = value.getStringItem("fieldTag");
			// extInfo.setItem("fieldTag", fieldTag);
			// }
			// 判断字段是否已存在
			String fieldName = value.getStringItem("fieldName");

			if (fieldsInfo.containsKey(fieldName)) {
				Logger.error("字段已存在，不能重复定义：" + fieldName);
				throw Error.INPARAMS_INVALID;
			}
			Integer fieldLen = value.getIntItem("fieldLen");
			// 累加总长度
			if (fieldLen > 0) {
				totalLen += fieldLen;
				if (separator != null) {
					totalLen += separator.length();
				}
			} else {
				Logger.error("定义字段长度不能小于0:" + fieldName);
				throw Error.INPARAMS_INVALID;
			}
			// 保存字段信息
			JavaDict info = value;
			// //xml
			// if(extInfo != null){
			// info.putAll(extInfo);
			// }
			fieldsInfo.setItem(fieldName, info);
			// 保存字段名称
			fieldsName.add(fieldName);
		}
		runTimeInfo.setItem("fieldsInfo", fieldsInfo);
		runTimeInfo.setItem("fieldsName", fieldsName);
		runTimeInfo.setItem("totalLen", totalLen);
	}

	/**
	 * 读取文件的入口方法
	 * 
	 * @param fileName
	 *            文件名包括路径名
	 * @param charset
	 * @param options
	 * @param runTimeInfo
	 * @return
	 * @throws Exception
	 */
	public JavaDict readFile(String fileName) throws Exception {
        // 回调
        fileParserInterFaceimpl.ReadBeginDealMethod();
        //
		String charset = options.getStringItem("encoding");
		// 读取结果
		JavaDict readResult;
		// 打开文件
		runTimeInfo.setItem("charset", charset);
		if (fileName == null || fileName.equals("")) {
			throw Error.INPARAMS_INVALID;
		}
		
		if (System.getProperty("os.name").toUpperCase()
				.contains("windows".toUpperCase())) {
			if (!(fileName.contains(":"))) {
				if (fileName.charAt(0) == '/') {
					fileName = System.getenv("HOME").replace('\\', '/')
							+ fileName;
				} else {
					fileName = System.getenv("HOME").replace('\\', '/') + "/"
							+ fileName;
				}
			}
		} else {
			if (System.getProperty("os.name").toUpperCase()
					.contains("Linux".toUpperCase())) {
				if (fileName.charAt(0) != '/') {
					fileName = System.getenv("HOME") + "/" + fileName;
				}
			}
		}
		Logger.info("开始读取文件:" + fileName);
		File fp = new File(fileName);
		if (!fp.exists()) {
			Logger.error("读取的文件不存在:" + fileName);
			throw Error.FILE_NOT_EXISTS;
		}
		if (fp.length() == 0) {
			Logger.error("读取的文件不能为空:" + fileName);
			throw new Error("error", "读取文件长度不能为空");
		}
		// 扩展模式 参数初始化
		JavaDict readModelOptions = new JavaDict();
		readModelOptions.setItem("isxml", false);
		readModelOptions.setItem("extMode", 0);
		readModelOptions.setItem("stopOnError", false);
		// //xml
		// if(options.getIntItem("fileType") == 2){
		// readModelOptions.setItem("isxml", true);
		// readModelOptions.setItem("extMode", 1);
		// readModelOptions.setItem("stopOnError", true);
		// }
		// 错误文件
		_debuginfo("fileNameErr:" + fp.getPath() + ".error.fail");
		runTimeInfo.setItem("fileNameErr", fp.getPath() + ".error.fail");
		runTimeInfo.setItem("fp", fp);
		int extMode = readModelOptions.getIntItem("extMode");
		if (extMode == 0) {
			// 获取读取模式 根据模式选择方法读取
			Integer readType = options.getIntItem("readType");
			Integer fileType = options.getIntItem("fileType");
			if (readType == 0) {// 按行读取
				Logger.info("开始按行读取");
				readResult = readFileByLine(readModelOptions, charset);
			} else if (readType == 1) {// 定长读取
				if (fileType == 0) {
					Logger.info("开始定长读取");
					readResult = readFilebySelfTotalLen(readModelOptions);
				} else {
					Logger.error("定长读取只能适用定长文件类型");
					throw Error.INPARAMS_INVALID;
				}
			} else {
				Logger.error("未知读取模式:" + readType);
				throw Error.INPARAMS_INVALID;
			}
			// }else if(extMode == 1){
			// // readFileXmlmodel(fp,readModelOptions,charset);
		} else {
			Logger.error("未知扩展模式:" + extMode);
			throw Error.INPARAMS_INVALID;
		}
		// 回调
		fileParserInterFaceimpl.ReadEndDealMethod();
		// 返回结果
		return readResult;
	}

	public JavaDict getFieldInfo(String fieldName, JavaDict fieldsInfo) {
		return fieldsInfo.getDictItem(fieldName);
	}

	/**
	 * 按字段长度读取 自动删除换号符
	 * 
	 * @param fp
	 * @param readModelOptions
	 * @throws Exception
	 */
	private JavaDict readFilebySelfTotalLen(JavaDict readModelOptions)
			throws Exception {
		String lineBreak = options.getStringItem("lineBreak", "");
		int totalLen = runTimeInfo.getIntItem("totalLen") + lineBreak.length();
		_debuginfo("lineBreak length:" + lineBreak.length());
		File fp = (File) runTimeInfo.get("fp");
		String charset = options.getStringItem("encoding");
		JavaDict resultDict = new JavaDict();
		boolean deal = false;
		String line = null;
		InputStream in = null;
		try {
			in = new FileInputStream(fp);
			int lastLine = 0;
			int Info;
			byte[] temp = new byte[totalLen];
			String preline = null;
			while ((Info = in.read(temp)) != -1) {
				lastLine++;				
				deBugInfoList = new JavaList();
				deBugInfoList.add("第"+(runTimeInfo.getIntItem("lineIndex", 0))+"行异常");
				if(preline == null){
					deBugInfoList.add("行信息:"+line);
				}else{
					deBugInfoList.add("行信息:"+preline);
				}
				if (preline != null && !preline.equals("")) {
					int result = _lineSelect(preline);
					if (result == 1) {
						preline = line;
						deal = false;
						continue;
					} else if (result == 2) {
						preline = line;
						deal = true;
						break;
					} else if (result == 0 && !deal) {
						resultDict = dealReadInfo(preline, readModelOptions,
								resultDict);
						if(fileParserInterFaceimpl.getController().equals("break")){
							_debuginfo("break跳出读取文件");
							deal = true;
							break;
						}if(fileParserInterFaceimpl.getController().equals("continue")){
							_debuginfo("continue跳过本行");
							fileParserInterFaceimpl.setController("");
							preline = line;
							deal = false;
							continue;
						}
						deal = true;
					} else if (result == 4) {
						deal = false;
						continue;
					}
				}
				line = new String(temp, 0, Info, charset);
				if(!"".equals(line.trim())){
					preline = line;
					deal = false;
				}else{
					deal = true;
				}
			}	
			runTimeInfo.setItem("lastLine", lastLine);
			int lineIndex = runTimeInfo.getIntItem("lineIndex", 0) + 1;
			_debuginfo("第" + lineIndex + "行:" + preline);
			runTimeInfo.setItem("lineIndex", lineIndex);
			if(deal == false){//如果还没处理
				if (lineIndex >= options.getIntItem("readBeginLineIndex")) {//如果不是还没开始读取的行
					int readLineCount = runTimeInfo.getIntItem("lineIndex") + 1;
					runTimeInfo.setItem("readLineCount", readLineCount);
					int optionsReadLineCount = options.getIntItem("readLineCount");
					if(!(optionsReadLineCount > 0 && readLineCount > optionsReadLineCount)){//如果不是已超出读取范围的行
						if (lineIndex == 1 && fileParserInterFaceimpl.getHeadDefine() != null) {
							runTimeInfo.setItem("lineType", 0);
							_setFieldsInfo();
						}else if(fileParserInterFaceimpl.getTailDefine() != null) {
							runTimeInfo.setItem("lineType", 2);
							_setFieldsInfo();
						}else if(fileParserInterFaceimpl.getBodyDefine() != null) {
							runTimeInfo.setItem("lineType", 1);
							_setFieldsInfo();
						}
						resultDict = dealReadInfo(preline, readModelOptions, resultDict);
					}
				}
			}
		} catch (Exception e) {
			Logger.error("按字节读取出错");
			for (int i = 0; i < deBugInfoList.size(); i++) {
				String eleStr = deBugInfoList.getStringItem(i);
				Logger.error(eleStr);
			}
			throw e;
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					Logger.error("inputStream close IOException:"
							+ e.getMessage());
					throw Error.SYSTEM_ERROR;
				}
			}
		}
		resultDict.setItem("outDict", fileParserInterFaceimpl.getOutDict());
		resultDict.setItem("outList", fileParserInterFaceimpl.getOutList());
		return resultDict;
	}

	/**
	 * 按行数读取
	 * 
	 * @param fp
	 * @param readModelOptions
	 * @param charset
	 * @return
	 * @throws Exception
	 */
	private JavaDict readFileByLine(JavaDict readModelOptions, String charset)
			throws Exception {
		Logger.info("a");
		BufferedReader bufin = null;
		File fp = (File) runTimeInfo.get("fp");
		String line = null;
		String preline = null;
		boolean deal = false;
		JavaDict resultDict = new JavaDict();
		try {
			bufin = new BufferedReader(new InputStreamReader(
					new FileInputStream(fp), charset));
			int lastLine = 0;
			Logger.info("b");
			while ((line = bufin.readLine()) != null) {
				lastLine++;
				deBugInfoList = new JavaList();
				deBugInfoList.add("第"+(runTimeInfo.getIntItem("lineIndex", 0))+"行异常");
				if(preline == null){
					deBugInfoList.add("行信息:"+line);
				}else{
					deBugInfoList.add("行信息:"+preline);
				}
				if (preline != null && !preline.equals("")) {
					int result = _lineSelect(preline);
					if (result == 1) {
						preline = line;
						deal = false;
						continue;
					} else if (result == 2) {
						preline = line;
						deal = true;
						break;
					} else if (result == 0 && !deal) {
						resultDict = dealReadInfo(preline, readModelOptions,
								resultDict);
						if(fileParserInterFaceimpl.getController().equals("break")){
							_debuginfo("break跳出读取文件");
							deal = true;
							break;
						}if(fileParserInterFaceimpl.getController().equals("continue")){
							_debuginfo("continue跳过本行");
							fileParserInterFaceimpl.setController("");
							preline = line;
							deal = false;
							continue;
						}
						deal = true;
					} else if (result == 4) {
						deal = false;
						continue;
					}
				}
				if(!"".equals(line.trim())){
					preline = line;
					deal = false;
				}else{
					deal = true;
				}
			}
			runTimeInfo.setItem("lastLine", lastLine);
			int lineIndex = runTimeInfo.getIntItem("lineIndex", 0) + 1;
			_debuginfo("第" + lineIndex + "行:" + preline);
			runTimeInfo.setItem("lineIndex", lineIndex);
			if(deal == false){//如果还没处理
				if (lineIndex >= options.getIntItem("readBeginLineIndex")) {//如果不是还没开始读取的行
					int readLineCount = runTimeInfo.getIntItem("lineIndex") + 1;
					runTimeInfo.setItem("readLineCount", readLineCount);
					int optionsReadLineCount = options.getIntItem("readLineCount");
					if(!(optionsReadLineCount > 0 && readLineCount > optionsReadLineCount)){//如果不是已超出读取范围的行
						if (lineIndex == 1 && fileParserInterFaceimpl.getHeadDefine() != null) {
							runTimeInfo.setItem("lineType", 0);
							_setFieldsInfo();
						}else if(fileParserInterFaceimpl.getTailDefine() != null) {
							runTimeInfo.setItem("lineType", 2);
							_setFieldsInfo();
						}else if(fileParserInterFaceimpl.getBodyDefine() != null) {
							runTimeInfo.setItem("lineType", 1);
							_setFieldsInfo();
						}
						resultDict = dealReadInfo(preline, readModelOptions, resultDict);
					}
				}
			}
		} catch(Exception e2){
			Logger.error("按行读取出错");
			for (int i = 0; i < deBugInfoList.size(); i++) {
				String eleStr = deBugInfoList.getStringItem(i);
				Logger.error(eleStr);
			}
			throw e2;
		} finally {
			if (bufin != null) {
				try {
					bufin.close();
				} catch (IOException e) {
					Logger.error("inputStream close IOException:"
							+ e.getMessage());
					throw Error.SYSTEM_ERROR;
				}
			}
		}
		resultDict.setItem("outDict", fileParserInterFaceimpl.getOutDict());
		resultDict.setItem("outList", fileParserInterFaceimpl.getOutList());
		return resultDict;
	}

	/**
	 * 
	 * @param line流读取到的一行
	 * @return 0继续执行 1跳过本次循环 2跳出循环
	 * @throws Exception
	 */
	private int _lineSelect(String line) throws Exception {
		int lineIndex = runTimeInfo.getIntItem("lineIndex", 0) + 1;
		_debuginfo("第" + lineIndex + "行:" + line);
		runTimeInfo.setItem("lineIndex", lineIndex);
		if (lineIndex < options.getIntItem("readBeginLineIndex")) {
			return 1;
		}
		int readLineCount = ++hasReadLine;
		int optionsReadLineCount = options.getIntItem("readLineCount");
		if (optionsReadLineCount > 0 && readLineCount > optionsReadLineCount) {
			return 2;
		}
		if (line == null || line.equals("")) {
			return 4;
		}
		// #行类型，0-文件头，1-文件体，2-文件尾
//		int lineType = runTimeInfo.getIntItem("lineType", -1);
		if (lineIndex == 1
				|| lineIndex == options.getIntItem("readBeginLineIndex")) {
			if (fileParserInterFaceimpl.getHeadDefine() != null) {
				runTimeInfo.setItem("lineType", 0);
				_setFieldsInfo();
			} else {
				runTimeInfo.setItem("lineType", 1);
				_setFieldsInfo();
			}
		}
		return 0;
	}

	private JavaDict dealReadInfo(String line, JavaDict readModelOptions,
			JavaDict resultDict) throws Exception {
		JavaDict lineDict = new JavaDict();
		JavaDict lineInfoDict = new JavaDict();
			
		
		// 读取记录
		try {
			lineInfoDict.setItem("line", line);
			lineInfoDict.setItem("lineIndex",
					runTimeInfo.getIntItem("lineIndex")-1);
			lineInfoDict
					.setItem("lineType", runTimeInfo.getIntItem("lineType"));
			_debuginfo("开始解析 第" + runTimeInfo.getIntItem("lineIndex")
					+ "行，解析格式为：lineType" + runTimeInfo.getIntItem("lineType"));
			
			nowLineIndex = runTimeInfo.getIntItem("lineIndex")-1;
			nowLine = line;
			
			lineDict = readRecord(line, lineInfoDict);
			
		} finally {
			// 还原文件体定义
			int lineType = runTimeInfo.getIntItem("lineType", -1);
			runTimeInfo.setItem("lineType", 1);
			if (lineType != runTimeInfo.getIntItem("lineType")) {
				_setFieldsInfo();
			}
			// JavaList fieldsInfoList = new JavaList();
			// int lineIndex = runTimeInfo.getIntItem("lineIndex");
			// fieldsInfoList.clear();
			// if(lineIndex == 0 &&
			// fileParserInterFaceimpl.getHeadDefine()!=null){
			// runTimeInfo.setItem("lineType", 1);
			// _setFieldsInfo();
			// }
		}
		_debuginfo("开始调用读取行信息处理器 第" + runTimeInfo.getIntItem("lineIndex") + "行");
		JavaDict result = fileParserInterFaceimpl.ReadLineInfoDealMethod(
				lineDict, lineInfoDict);
		if (result.getStringItem("status").equals("success")) {
			// 累加成功行数
			int successCount = runTimeInfo.getIntItem("successCount", 0);
			int lineIndex = runTimeInfo.getIntItem("lineIndex");
			runTimeInfo.setItem("successCount", successCount + 1);
			JavaDict lineMsg = result.getDictItem("successResult", null);
			if (lineMsg != null && lineMsg.size() != 0) {
				resultDict.setItem(lineIndex, lineMsg);
			}
			JavaDict fileMsg = resultDict
					.getDictItem("fileMsg", new JavaDict());
			if (fileMsg != null && fileMsg.size() != 0) {
				fileMsg.putAll(result.getDictItem("fileMsg", new JavaDict()));
				resultDict.setItem("fileMsg", fileMsg);
			}
		} else if (result.getStringItem("status").equals("error")) {
			for (int i = 0; i < deBugInfoList.size(); i++) {
				String eleStr = deBugInfoList.getStringItem(i);
				Logger.error(eleStr);
			}
			// 累加失败行数
			int failedCount = runTimeInfo.getIntItem("failedCount", 0);
			runTimeInfo.setItem("failedCount", failedCount + 1);

			 //写错误文件
			 String fileNameErr = runTimeInfo.getStringItem("fileNameErr");
			 if(fileNameErr != null){
				 FileWriter fw = new FileWriter(fileNameErr, true);
				 PrintWriter pw = null;
				 try{
					 pw = new PrintWriter(fw);
					 StringBuffer errorline = new StringBuffer();
					 errorline.append(new Date() + "  解析文件失败，行号:"+
					 String.valueOf(runTimeInfo.getIntItem("lineIndex")-1));
					 if(result.getStringItem("errorMsg")!=null){
					 errorline.append(" ,错误信息: "+ result.getStringItem("errorMsg"));
						Logger.error(new Date() + "  解析文件失败，行号:"
								+ String.valueOf(runTimeInfo.getIntItem("lineIndex") - 1)+" ,错误信息: "+ result.getStringItem("errorMsg"));
				 }
					 pw.println(errorline);
					 pw.flush();
				 }finally{
					 try{
						 if(pw != null){
							 fw.flush();
							 pw.close();
							 fw.close();
						 }
					 }catch(IOException e){
						 throw Error.FILE_WRITE_ERROR;
					 }
				 }
			 }
		} else {
			Logger.error("读取行信息处理器出参有误:" + result.getStringItem("status"));
			throw Error.INPARAMS_INVALID;
		}
		return resultDict;
	}

	private JavaDict readRecord(String line, JavaDict lineInfoDict)
			throws IOException, Error {
		// 字段内容（记录解析结果）
		JavaDict lineDict = new JavaDict();
		// 初始化参数
		String fieldName = null;
		// 当前位置的索引
		int posIndex = 0;

		String[] valuesList = null;
		JavaList fieldsName = runTimeInfo.getListItem("fieldsName");
		JavaDict fieldsInfo = runTimeInfo.getDictItem("fieldsInfo");
		String charset = options.getStringItem("encoding");
		String separator = options.getStringItem("separator", null);
		byte[] lineBytes = null;
		for (int i = 0; i < fieldsName.size(); i++) {
			try {
				// 获取字段信息
				fieldName = fieldsName.getStringItem(i);
				JavaDict fieldInfo = fieldsInfo.getDictItem(fieldName);
				Integer fieldLen = fieldInfo.getIntItem("fieldLen");
				if (fieldLen <= 0) {
					throw Error.INPARAMS_INVALID;
				}
				String fieldValue = null;
				if (options.getIntItem("fileType") == 0) {// 文件类型 0-定长
					if (i == 0) {
						lineBytes = line.getBytes(charset);
					}
					// 定长读取，且长度为动态长度，则从文件读取
					if (options.getIntItem("readType") == 1
							&& fieldInfo.getIntItem("fieldLen") < 0) {

					} else {// 其他情况则直接读取
						fieldValue = new String(lineBytes, posIndex,
								fieldInfo.getIntItem("fieldLen"), charset);
					}
					posIndex += fieldInfo.getIntItem("fieldLen");
					if (separator != null) {
						posIndex += separator.length();
					}
				} else if (options.getIntItem("fileType") == 1) {// 文件类型 1变长
					if (i == 0) {
						valuesList = line.split(separator, -1);
						if (valuesList.length < fieldsName.size()) {
							Logger.error("字段个数少于定义个数:" + line);
							Logger.error("字段数(%s)<定义数(%s)", valuesList.length,
									fieldsName.size());
							deBugInfoList = new JavaList();
							throw Error.FIELDSCOUNT_TOOSMALL;
						}
					}
					fieldValue = valuesList[i];
					// }else if(options.getIntItem("fileType") == 2){//文件类型 2XML
					// fieldValue =
					// line.get(fieldInfo.getStringItem("fieldTag"));
				} else {
					Logger.error("未知文件类型:" + options.getStringItem("fileType"));
					throw Error.INPARAMS_INVALID;
				}
				JavaDict fieldTypeMethodMap = options
						.getDictItem("fieldTypeMethodMap");
				// 解析字段值
				Object method = fieldTypeMethodMap.getItem(
						fieldInfo.getStringItem("fieldType"), null);
				nowfieldKV = fieldInfo.getStringItem("fieldRemark", "获取字段名失败")+"("+fieldValue+")";
				deBugInfoList.add("字段读取 字段名：(" + fieldInfo.getStringItem("fieldRemark") + ")  字段值：(" + fieldValue
						+ ")");
				if (method instanceof Method) {
					Object[] params = new Object[4];
					params[0] = 0;
					params[1] = fieldValue;
					params[2] = fieldInfo;
					params[3] = options;
					try {
						fieldValue = (String) ((Method) method).invoke(null,
								params);
					} catch (Exception e) {
						Logger.error("字段读取错误 字段名：(" + fieldInfo.getStringItem("fieldRemark") + ")  字段值：("
								+ fieldValue + ")  记录序号:("
								+ lineInfoDict.getStringItem("lineIndex") + ")");
						Error ee = null;
						try {
							ee = (Error) e.getCause();
						} catch (Exception e1){
							throw Error.VALUE_FORMAT_ERROR;
						}
						JavaList errorList = new JavaList();
						errorList.add(Error.VALUE_EMPTY.errorCode);
						errorList.add(Error.VALUE_TOOLONG.errorCode);
						errorList.add(Error.VALUE_NOT_NUMBER.errorCode);
						if (errorList.contains(ee.errorCode)) {
							throw ee;
						}else{
							throw Error.VALUE_FORMAT_ERROR;
						}
					}
				} else {
					Logger.error("未知字段类型或没有该类型处理方法"
							+ fieldInfo.getStringItem("fieldType"));
				}

				// 保存结果
				_debuginfo("字段读取 字段名：(" + fieldInfo.getStringItem("fieldRemark") + ")  字段值：(" + fieldValue
						+ ")");
				lineDict.setItem(fieldName, fieldValue);
			} catch (Error e) {
				Logger.error("处理字段出错：" + fieldName);
				throw e;
			}
		}
		// 返回结果
		return lineDict;
	}

	private boolean _createFile(String fileName) throws Error {
		File file = new File(fileName);
		boolean reWriteFile = options.getBooleanItem("reWriteFile", true);
		if (file.exists()) {
			Logger.info("文件已存在:" + fileName);
		} else if (fileName.endsWith(File.separator)) {
			Logger.error("文件不能为目录:" + fileName);
		}
		if (!file.getParentFile().exists()) {
			if (!file.getParentFile().mkdirs()) {
				Logger.error("目录创建失败:" + file.getParentFile());
				throw Error.SYSTEM_ERROR;
			}
		}
		if (!reWriteFile) {
			try {
				if (file.createNewFile()) {
					return true;
				} else {
					Logger.error("创建文件失败:" + fileName);
				}
			} catch (IOException e) {
				throw Error.SYSTEM_ERROR;
			}
		} else {
			Logger.info("重写原文件：" + fileName);
		}
		return false;
	}
	
	public JavaDict writeFileWithIter(String fileName, Iterator<JavaDict> iter, JavaDict headDict, JavaDict tailDict) throws Error {
        // 格式定义
        JavaList headDefine = fileParserInterFaceimpl.getHeadDefine();
        JavaList bodyDefine = fileParserInterFaceimpl.getBodyDefine();
        JavaList tailDefine = fileParserInterFaceimpl.getTailDefine();
        // 清理运行信息
        runTimeInfo.clear();
        runTimeInfo.setItem("preLineType", -1);
        // 文件对象
	    FileOutputStream out = null;
	    try {
	        // 序号
	        int lineIndex = -1;
	        // 打开文件
	        out = new FileOutputStream(fileName);
	        // 头
	        if (headDefine != null && headDict != null) {
	            lineIndex++;
	            _procWithIter(out, lineIndex, 0, headDict);
	        }
	        // 逐行处理
	        while (iter.hasNext()) {
	            JavaDict lineDict = iter.next();
	            try {
	                lineIndex++;
	                _procWithIter(out, lineIndex, 1, lineDict);
	            }
	            catch (Exception ex) {
	                // 记录引发错误的数据
	                Logger.error("处理数据出错：%s\n%s", lineIndex, lineDict);
	                // 重新抛出
	                throw Error.getInstance(ex);
	            }
	        }
            // 尾
            if (tailDefine != null && tailDict != null) {
                lineIndex++;
                _procWithIter(out, lineIndex, 2, tailDict);
            }
            // 重写头
            boolean rewriteHeader = options.getBooleanItem("rewriteHeader", false);
            if (rewriteHeader) {
                _procWithIter(out, lineIndex, 0, headDict);
            }
        } catch (Exception ex) {
            throw Error.getInstance(ex);
        }
	    finally {
	        if (out != null) {
	            try {
                    out.close();
                } catch (IOException ex) {
                    Logger.exception(ex);
                    Logger.error("关闭文件异常，忽略");
                }
	        }
	    }
	    
	    return null;
	}
	
	private void _procWithIter(FileOutputStream out, int lineIndex, int lineType, JavaDict lineDict) throws Exception {
        // 编码
        String encoding = options.getStringItem("encoding");
        // 换行符
        String lineBreak = options.getStringItem("lineBreak", "");
        // 处理行类型
        runTimeInfo.setItem("dataSize", -1);
        if (lineType == 2) {
            // 标记，后续proc会检测，一致则认为是文件尾
            runTimeInfo.setItem("dataSize", lineIndex);
        }
        // 回调（适应旧协议）
        /** lineInfoDict */
        JavaDict lineInfoDict = new JavaDict();
        lineInfoDict.put("lineType", lineType);
        /** fieldDict */
        JavaDict fieldDict = new JavaDict();
        fieldDict.put("lineDict", lineDict);
        fieldDict.put("lineInfoDict", lineInfoDict);
        fileParserInterFaceimpl.WriteLineFieldMap(fieldDict);
        // 处理数据
        JavaDict result = _proc(lineIndex, lineDict);
        String writeline = result.getStringItem("writeline");
        // 写入文件
        out.write((writeline + lineBreak).getBytes(encoding));
	}

	// 写入文件
	public JavaDict writeFile(String fileName, JavaList srcData)
			throws Exception {
		_debuginfo("传入行数", srcData.size());
		String charset = options.getStringItem("encoding");
		if (fileParserInterFaceimpl == null) {
			Logger.error(" 初始化失败：没有加载FileParserInterFace实现类");
			throw Error.INPARAMS_INVALID;
		}
		runTimeInfo.clear();
		runTimeInfo.setItem("preLineType", -1);
		boolean reWriteFile = options.getBooleanItem("reWriteFile", true);
		String lineBreak = options.getStringItem("lineBreak", "");

		FileOutputStream out = null;

		if (_createFile(fileName)) {
			Logger.info("创建文件成功", fileName);
			out = new FileOutputStream(fileName);
		} else {
			if (!reWriteFile) {
				out = new FileOutputStream(fileName, true);
				File file = new File(fileName);
				if(file.length() !=0 && options.getBooleanItem("LineBreakreWrite", true)){
					out.write(lineBreak.getBytes(charset));
				}
			} else {
				out = new FileOutputStream(fileName, false);
			}
		}
		JavaDict resultDict = new JavaDict();
		// #XML
		// if self.options["fileType"] == "2":
		// return self._writeXMLFile(fileName, dataSrc, callback, hdrDataSrc)
		try {
			runTimeInfo.setItem("line", null);
			runTimeInfo.setItem("lastLine", 0);
			runTimeInfo.setItem("dataSize", srcData.size());
			Boolean writeLineInfoDealMethod = options.getBooleanItem(
					"WriteLineInfoDealMethod", false);
			boolean lastFlag = false;
			for (int i = 0; i < srcData.size(); i++) {
				if (i == srcData.size()) {
					lastFlag = true;
				}
				JavaDict result = _proc(i, srcData.getDictItem(i));
				String writeline = result.getStringItem("writeline");
				if (writeLineInfoDealMethod) {
					resultDict.setItem(i,
							resultDict.getDictItem("successResult"));
					JavaDict fileMsg = result.getDictItem("fileMsg",
							new JavaDict());
					fileMsg.putAll(fileMsg);
					resultDict.setItem("fileMsg", fileMsg);
				}
				_debuginfo("第" + i + "行:" + writeline);
				if (lastFlag &&!options.getBooleanItem("lastlineBreak", false)) {
					out.write(writeline.getBytes(charset));
				} else {
					out.write((writeline + lineBreak).getBytes(charset));
				}
			}
			out.flush();
		} catch (Exception e) {
			throw e;
		} finally {
			if (out != null) {
				out.close();
			}
		}
		return resultDict;
	}

	// 写入文件
	public JavaDict writeBigFile(String fileName)
			throws Exception {
		String charset = options.getStringItem("encoding");
		if (fileParserInterFaceimpl == null) {
			Logger.error(" 初始化失败：没有加载FileParserInterFace实现类");
			throw Error.INPARAMS_INVALID;
		}
		Logger.info("--------------------------");
		runTimeInfo.clear();
		runTimeInfo.setItem("preLineType", -1);
		boolean reWriteFile = options.getBooleanItem("reWriteFile", true);
		boolean issetHead = (fileParserInterFaceimpl.getHeadDefine()!=null);
		boolean issetBody = (fileParserInterFaceimpl.getBodyDefine()!=null);
		boolean issetTail = (fileParserInterFaceimpl.getTailDefine()!=null);

		FileOutputStream out = null;
		String lineBreak = options.getStringItem("lineBreak", "");

		if (_createFile(fileName)) {
			Logger.info("创建文件成功", fileName);
			out = new FileOutputStream(fileName);
		} else {
			if (!reWriteFile) {
				out = new FileOutputStream(fileName, true);
				File file = new File(fileName);
				if(file.length() !=0 && options.getBooleanItem("LineBreakreWrite", true)){
					out.write(lineBreak.getBytes(charset));
				}
			} else {
				out = new FileOutputStream(fileName, false);
			}
		}

		Boolean writeLineInfoDealMethod = options.getBooleanItem(
				"WriteLineInfoDealMethod", false);
		JavaDict resultDict = new JavaDict();
		try {
			if (issetHead) {
				fileParserInterFaceimpl.initHeadDateDict();
				JavaDict headDict = fileParserInterFaceimpl.getHeadDateDict();
				JavaDict result = _proc(0, headDict);
				String writeline = result.getStringItem("writeline");
				if (writeLineInfoDealMethod) {
					resultDict.setItem(0,
							resultDict.getDictItem("successResult"));
					JavaDict fileMsg = result.getDictItem("fileMsg",
							new JavaDict());
					fileMsg.putAll(fileMsg);
					resultDict.setItem("fileMsg", fileMsg);
				}
				_debuginfo("第0行:" + writeline);
				
				out.write((writeline).getBytes(charset));
				
//				if(!options.getBooleanItem("lastlineBreak", false) && !issetBody && !issetTail){
//					out.write((writeline).getBytes(charset));
//				}else{
//					out.write((writeline + lineBreak).getBytes(charset));
//				}
			}
			fileParserInterFaceimpl.initselectInfoDict();
			if(fileParserInterFaceimpl.getSelectDict()==null){
				return resultDict;
			}
			runTimeInfo.setItem("line", null);
			runTimeInfo.setItem("lastLine", 0);
			//lineIndex == runTimeInfo.getIntItem("dataSize");
			JavaDict selectInfoDict = fileParserInterFaceimpl.getSelectDict();
			
			String poolName = selectInfoDict.getStringItem("poolName", "");
			String tableName = selectInfoDict.getStringItem("tableName", null);
			JavaList selectList = selectInfoDict.getListItem("selectList", new JavaList());
			JavaDict conditions = selectInfoDict.getDictItem("conditions", new JavaDict());
			String pageSize = selectInfoDict.getStringItem("pageSize", options.getStringItem("pageSize"));
			String orderByValue = selectInfoDict.getStringItem("orderByValue", "");
			
			//测试
//			conditions = new JavaDict();
//		    conditions.put("payPathWorkRound", new JavaDict("=", new JavaList("B201801040019")));
//		    conditions.put("pathProcStatus", new JavaDict("=", new JavaList("1")));
//		    conditions.put("payPathWkDate", new JavaDict("=", new JavaList("20180104")));
		    //测试
			
			if(tableName == null || tableName.endsWith("")){
				Logger.info("入参表名为空：%s", tableName);
			}
			int rowCount = 0;
			try{
				JavaDict REQ = new JavaDict();
				REQ.setItem(Conf.COMM_ISDISPLAYLOG_FLAG, "N");
				JavaList tmp = Jdbc.standardSelect(REQ,poolName, tableName, new JavaList("count(*)"), conditions, null, null, 0);
				rowCount = Integer.parseInt(tmp.getListItem(0).getItem(0) + "");
			}catch(Error e){
				e.printStackTrace();
				throw Error.DATABASE_ERROR;
			}
			
			if(!issetHead){
				
			}else if(options.getBooleanItem("lastlineBreak", false)){
				out.write(lineBreak.getBytes(charset));
			}else if(rowCount != 0){
				out.write(lineBreak.getBytes(charset));
			}
			
			int pageCount = Integer.valueOf(rowCount)/Integer.valueOf(pageSize);
			pageCount+=1;
			Logger.info("数据总数：%s，分页数：%s，每页上限：%s", rowCount, pageCount, pageSize);
			for(int j = 1; j < pageCount+1;j++){
				Logger.info("查询页：%s", j);
				//分页查询
				JavaList pageDate = new JavaList();
				try {
					pageDate = Jdbc.pageQuery(null,poolName, tableName, selectList, conditions, String.valueOf(j), pageSize, orderByValue);
				} catch (Error e) {
					Logger.error("分页查询错误：当前页数%s", j);
					throw Error.DATABASE_ERROR;
				}
				JavaList srcData = new JavaList();//**************************
				for (int n = 0; n < pageDate.size(); n++) {
					JavaDict bodyDict = new JavaDict();
					JavaList bodyDate = pageDate.getListItem(n);
					for (int m = 0; m < selectList.size(); m++) {
						if(bodyDate.get(m) == null){
							bodyDate.set(m, "");
						}
						bodyDict.setItem(selectList.get(m), bodyDate.get(m));
					}
					bodyDict = fileParserInterFaceimpl.WriteLineFieldMap(bodyDict);
					srcData.add(bodyDict);
				}
				for (int i = 0; i < srcData.size(); i++) {
					boolean lastFlag = false;
					int lineIndex = (j-1)*Integer.valueOf(pageSize)+i;
					if(issetHead){
						lineIndex+=1;
					}
					
					if (issetHead &&  lineIndex == Integer.valueOf(rowCount)) {
						lastFlag = true;
					}
					if(!issetHead && lineIndex == Integer.valueOf(rowCount)-1) {
						lastFlag = true;
					}
					JavaDict result = _proc(lineIndex, srcData.getDictItem(i));
					String writeline = result.getStringItem("writeline");
					if (writeLineInfoDealMethod) {
						resultDict.setItem(lineIndex,
								resultDict.getDictItem("successResult"));
						JavaDict fileMsg = result.getDictItem("fileMsg",
								new JavaDict());
						fileMsg.putAll(fileMsg);
						resultDict.setItem("fileMsg", fileMsg);
					}
					_debuginfo("第" + lineIndex + "行:" + writeline);
					if (lastFlag) {
						if (!options.getBooleanItem("lastlineBreak", false) && !issetTail) {
							Logger.info("不添加换行符");
							out.write(writeline.getBytes(charset));
						} else {
							Logger.info("添加换行符");
							out.write((writeline + lineBreak).getBytes(charset));
						}
					} else {
						Logger.info("添加换行符");
						out.write((writeline + lineBreak).getBytes(charset));
					}
				}
				out.flush();
			}
			if (issetTail) {
				fileParserInterFaceimpl.initTailDateDict();
				int lineIndex2 = rowCount;
				if(issetHead){
					lineIndex2 += 1;
				}
				runTimeInfo.setItem("dataSize", lineIndex2);
				JavaDict tailDict = fileParserInterFaceimpl.getTailDateDict();
				JavaDict result = _proc(lineIndex2, tailDict);
				String writeline = result.getStringItem("writeline");
				_debuginfo("第" + lineIndex2 + "行:" + writeline);
				if(options.getBooleanItem("lastlineBreak", false)){
					out.write((writeline + lineBreak).getBytes(charset));
				}else{
					out.write((writeline).getBytes(charset));
				}
				
			}
			out.flush();
		} catch (Exception e) {
			throw e;
		} finally {
			if (out != null) {
				out.close();
			}
		}
		return resultDict;
	}
	
	
	private JavaDict _proc(int lineIndex, JavaDict data) throws Exception {
		if (fileParserInterFaceimpl == null) {
			Logger.error(" 初始化失败：没有加载FileParserInterFace实现类");
			throw Error.INPARAMS_INVALID;
		}
		int lineType = 1;
		runTimeInfo.setItem("lineType", 1);
		if (lineIndex == 0 && fileParserInterFaceimpl.getHeadDefine() != null) {
			lineType = 0;
			runTimeInfo.setItem("lineType", 0);
		}
		if (lineIndex == runTimeInfo.getIntItem("dataSize",-1)
				&& fileParserInterFaceimpl.getTailDefine() != null) {
			lineType = 2;
			runTimeInfo.setItem("lineType", 2);
		}
		if (runTimeInfo.getIntItem("preLineType", -1) != lineType) {
			_setFieldsInfo();
		}
		runTimeInfo.setItem("preLineType", lineType);
		JavaDict lineInfoDict = new JavaDict();
		lineInfoDict.setItem("lineType", lineType);
		lineInfoDict.setItem("lineIndex", lineIndex);
		Object line = writeRecord(data);
		JavaDict resultDict = new JavaDict();
		if (options.getBooleanItem("WriteLineInfoDealMethod", false)) {
			if (line instanceof String) {
				resultDict = fileParserInterFaceimpl.WriteLineInfoDealMethod(
						(String) line, data, lineInfoDict);
			} else {
				resultDict = fileParserInterFaceimpl.WriteLineInfoDealMethod(
						(JavaList) line, data, lineInfoDict);
			}
			if (resultDict.getStringItem("status").equals("success")) {
				// 累加成功行数
				int successCount = runTimeInfo.getIntItem("successCount", 0);
				runTimeInfo.setItem("successCount", successCount + 1);
				resultDict.setItem("writeline",
						resultDict.getStringItem("writeline"));
			} else if (resultDict.getStringItem("status").equals("error")) {
				// 累加失败行数
				int failedCount = runTimeInfo.getIntItem("failedCount", 0);
				runTimeInfo.setItem("failedCount", failedCount + 1);
				// 写错误文件
			}
		} else {
			resultDict.setItem("writeline", (String) line);
		}
		return resultDict;
	}

	/**
	 * 写入记录
	 * 
	 * @param lineDict
	 * @return
	 * @throws Error
	 * @throws InvocationTargetException
	 * @throws IllegalArgumentException
	 * @throws Exception
	 */
	private Object writeRecord(JavaDict lineDict) throws Error, Exception {
		int lineIndex = runTimeInfo.getIntItem("lineType");
		Boolean joinList = options.getBooleanItem("WriteRecordReturnList",
				false);
		JavaList lineList = new JavaList();
		JavaList fieldsName = runTimeInfo.getListItem("fieldsName", null);
		for (int i = 0; i < fieldsName.size(); i++) {
			String fieldName = fieldsName.getStringItem(i);
			try {
				JavaDict fieldsInfo = runTimeInfo.getDictItem("fieldsInfo",
						null);
				// 获取字段信息
				JavaDict fieldInfo = fieldsInfo.getDictItem(fieldName);
				// int fieldLen = fieldInfo.getIntItem("fieldLen");
				// Logger.info("lineDict:%s", lineDict);
				// Logger.info("fieldName:%s", fieldName);
				String fieldValue = lineDict.getStringItem(fieldName, "");
				if (fieldValue.equals("")) {
					_debuginfo(fieldName + "为空或不存在于入参中");
				}
				// 转换编码
//				String encoding = options.getStringItem("encoding");
				// if(encoding != null && encoding != Conf.ENCODING_UPPS){
				// fieldValue = Strutil.StringEncode(fieldValue,encoding);
				// }
				// 解析字段值
				
				JavaDict fieldTypeMethodMap = options.getDictItem(
						"fieldTypeMethodMap", null);
				String fieldType = fieldInfo.getStringItem("fieldType");
				if (!fieldTypeMethodMap.containsKey(fieldType)) {
					Logger.error("字段名(%s)未知字段类型(%)" , fieldInfo.getStringItem("fieldRemark", "获取字段名失败") , fieldType);
					throw Error.INPARAMS_INVALID;
				}
				nowfieldKV = fieldInfo.getStringItem("fieldRemark", "获取字段名失败")+"("+fieldValue+")";
				Method method = (Method) fieldTypeMethodMap.getItem(
						fieldInfo.getStringItem("fieldType"), null);
				_debuginfo("字段名:"
						+ fieldInfo.getStringItem("fieldRemark", "获取字段名失败")
						+ "   字段类型:" + fieldInfo.getStringItem("fieldType")
						+ "  字段值: (" + fieldValue + ")");
				Object[] params = new Object[4];
				params[0] = 1;
				params[1] = fieldValue;
				params[2] = fieldInfo;
				params[3] = options;
				try {
					fieldValue = (String) method.invoke(null, params);
					// fieldValue = Strutil.StringDecode(fieldValue, encoding);
				} catch (InvocationTargetException e) {
					Logger.error("第%s行处理出错:%s", lineIndex, fieldInfo.getStringItem("fieldRemark", "获取字段名失败"));
					e.printStackTrace();
					throw Error.VALUE_FORMAT_ERROR;
				}
				_debuginfo("字段类型处理后:" + fieldInfo.getStringItem("fieldType")
						+ "  字段值: (" + fieldValue + ")");
				lineList.add(fieldValue);
			} catch (Error e) {
				throw e;
			}
		}
		if (joinList) {
			return lineList;
		}
		String outStr = Strutil.listToString(lineList,
				options.getStringItem("separator"));
		if (options.getBooleanItem("lineEndSeparator")) {
			outStr += options.getStringItem("separator");
		}
		return outStr;
	}
	
	public Error getImplError() throws Error{
		if (fileParserInterFaceimpl == null) {
			Logger.error(" 初始化失败：没有加载FileParserInterFace实现类");
			throw Error.INPARAMS_INVALID;
		}
		return fileParserInterFaceimpl.getImplError();
	}

	// 检查数值
	public static String _checkNumberValue(String value, JavaDict fieldInfo,
			JavaDict options) throws Error {
		if (value.equals("")) {
			return "0";
		}
		if (!_isNumeric(value)) {
			Logger.error("非数字类型:" + fieldInfo.getStringItem("fieldRemark"));
			throw Error.VALUE_NOT_NUMBER;
		}
		return Integer.valueOf(value).toString();
	}

	// 判断是否纯整数
	public static boolean _isNumeric(String str) {
		for (int i = 0; i < str.length(); i++) {
			if (!Character.isDigit(str.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	// 检查超长值
	public static void _checkTooLongValue(String value, JavaDict fieldInfo,
			JavaDict options) throws Error {
		Integer fieldLen = 0;
		try {
			fieldLen = value.getBytes(options
					.getStringItem("encoding", "UTF-8")).length;
		} catch (UnsupportedEncodingException e) {
			Logger.error("字符串转换为byte[]出错", value);
			e.printStackTrace();
		}
		Integer fieldMaxLen = fieldInfo.getIntItem("fieldLen");
		if (fieldMaxLen > 0 && fieldLen > fieldMaxLen) {
			Logger.error("字段值过长:" + value);
			Logger.error("字段名:" + fieldInfo.getStringItem("fieldRemark"));
			Logger.error("字段解析长度:" + fieldLen);
			Logger.error("字段最大长度:" + fieldMaxLen);
			if(options.getBooleanItem("checkLongValue", true)){
				throw Error.VALUE_TOOLONG;
			}
		}
	}

	// 检查空值
	public static String _checkEmptyValue(String value, JavaDict fieldInfo,
			JavaDict options) throws Error {
		if (!value.equals("")) {
			return value;
		}
		value = fieldInfo.getStringItem("fieldDefValue");
		if (options.getIntItem("allIsOptional") == 0 && value.equals("")) {
			String fieldMust = fieldInfo.getStringItem("fieldMust");
			int flag = -1;
			if (options.getDictItem("fieldMustMap") != null) {
				flag = options.getDictItem("fieldMustMap").getIntItem(
						fieldMust, -1);
			}
			if (!(flag == 0 || flag == 1)) {
				Logger.error("非法必送标识:" + fieldInfo.getStringItem("fieldRemark")
						+ " " + flag);
				throw Error.INPARAMS_INVALID;
			}
			if (flag == 1) {
				Logger.error("Warning: 必送字段字段值为空 " + fieldInfo.getStringItem("fieldRemark") + "为空");
				if(options.getBooleanItem("emptyValueThrowError",false)){
					throw Error.VALUE_EMPTY;
				}
			}
		}
		return value;
	}

	// P-原值型
	public static String method_P(int methodType, String value,
			JavaDict fieldInfo, JavaDict options) {
		return value;
	}

	// S-字符型
	public static String method_S(int methodType, String value,
			JavaDict fieldInfo, JavaDict options) throws Error {
		if (methodType == 0) {// 读取
			// 去空格
			value = value.trim();
			// 检查空值
			value = _checkEmptyValue(value, fieldInfo, options);
			// 检查超长值
			_checkTooLongValue(value, fieldInfo, options);
		} else if (methodType == 1) {// 写入
			// 检查空值
			value = _checkEmptyValue(value, fieldInfo, options);
			// 检查超长值
			_checkTooLongValue(value, fieldInfo, options);
			// 定长类型
			if (options.getIntItem("fileType") == 0) {
				int len = -1;
				try {
					len = fieldInfo.getIntItem("fieldLen")
							+ value.length()
							- value.getBytes(options.getStringItem("encoding")).length;
				} catch (UnsupportedEncodingException e) {
					Logger.error("字符串转换为byte[]出错", value);
					e.printStackTrace();
				}
				if (!(len == -1)) {
					String justFlag = options.getStringItem("justFlag");
					if (justFlag.equals("R")) {
						value = Strutil.fillchar(value, len, ' ', 'l');
					} else {
						value = Strutil.fillchar(value, len, ' ', 'r');
					}
				}
			}
		} else {
			Logger.error("字符型S处理出错");
			throw Error.INPARAMS_INVALID;
		}
		// 返回结果
		return value;
	}

	// N-数值型
	public static String method_N(int methodType, String value,
			JavaDict fieldInfo, JavaDict options) throws Error {
		if (methodType == 0) {// 读取
			// 去空格
			value = value.trim();
			// 检查空值
			value = _checkEmptyValue(value, fieldInfo, options);
			// 检查数值
			value = _checkNumberValue(value, fieldInfo, options);
			// 检查超长值
			_checkTooLongValue(value, fieldInfo, options);
		} else if (methodType == 1) {// 写入
			// 检查空值
			value = _checkEmptyValue(value, fieldInfo, options);
			// 检查超长值
			_checkTooLongValue(value, fieldInfo, options);
			// 定长类型
			if (options.getIntItem("fileType") == 0) {
				int len = fieldInfo.getIntItem("fieldLen") + value.length()
						- value.getBytes().length;
				value = Strutil.fillchar(value, len, '0', 'l');
			}
		} else {
			Logger.error("数值型N处理出错");
			throw Error.INPARAMS_INVALID;
		}
		// 返回结果
		return value;
	}
	
	// N0-带0数值型
	public static String method_N0(int methodType, String value,
			JavaDict fieldInfo, JavaDict options) throws Error {
		value = method_N(methodType, Amount.deletePreZero(value), options, options);
		return value;
	}
	
	// A-金额型
	public static String method_A(int methodType, String value,
			JavaDict fieldInfo, JavaDict options) throws Error {
		if (methodType == 0) {// 读取
			// 去空格
			value = value.trim();
			// 检查空值
			value = _checkEmptyValue(value, fieldInfo, options);
//			// 分转元
//			value = Amount.fenToYuan(value);
			// 检查超长值
			_checkTooLongValue(value, fieldInfo, options);
		} else if (methodType == 1) {// 写入
			// 检查空值
			value = _checkEmptyValue(value, fieldInfo, options);
//			// 分转元
//			value = Amount.fenToYuan(value, false);
			// 检查超长值
			_checkTooLongValue(value, fieldInfo, options);
			// 定长类型
			if (options.getIntItem("fileType") == 0) {
				int len = -1;
				try {
					len = fieldInfo.getIntItem("fieldLen")
							+ value.length()
							- value.getBytes(options.getStringItem("encoding",
									"UTF-8")).length;
				} catch (UnsupportedEncodingException e) {
					Logger.error("字符串转换为byte[]出错", value);
					e.printStackTrace();
				}
				if (!(len == -1)) {
					if (value.charAt(0) == '0') {
						value = "-"
								+ Strutil.fillchar(
										value.substring(1, value.length()),
										len, '0', 'r');
					} else {
						value = Strutil.fillchar(value, len, '0', 'l');
					}
				}
			}
		} else {
			Logger.error("金额型处理出错");
			throw Error.INPARAMS_INVALID;
		}
		// 返回结果
		return value;
	}
	
	//A0-带0金额型
	public static String method_A0(int methodType, String value,
			JavaDict fieldInfo, JavaDict options) throws Error {
		value = method_A(methodType, Amount.deletePreZero(value), options, options);
		return value;
	}
	
	// AS-金额型(带符号)
	public static String method_AS(int methodType, String value,
			JavaDict fieldInfo, JavaDict options) throws Error {
		value = method_A(methodType, value, options, options);
		if (options.getIntItem("fileType") == 0) {
			if (value.charAt(0) == '0') {
				value = "+" + value.substring(1, value.length());
			} else if (value.startsWith("-")) {
//				value = "+" + value;
			} else {
				throw Error.VALUE_TOOLONG;
			}
		}
		// 返回结果
		return value;
	}
	
	
	// 日期时间
	private static String _method_DateTime(int methodType, String value,
			JavaDict fieldInfo, JavaDict options, String dtFormat,
			String bankFormat) throws Error {
		if (bankFormat == null) {
			bankFormat = "yyyyMMdd";
		}
		if (methodType == 0) {// 读取
			// 去空格
			value = value.trim();
			// 检查空值
			value = _checkEmptyValue(value, fieldInfo, options);
			if (value != null && !value.equals("")) {
				// 日期格式转换
				value = DateUtil.convertDateFormat(value, dtFormat, bankFormat);
			}
			// 检查超长值
			_checkTooLongValue(value, fieldInfo, options);
		} else if (methodType == 1) {// 写入
			// 检查空值
			value = _checkEmptyValue(value, fieldInfo, options);
			// 分转元
			value = DateUtil.convertDateFormat(value, bankFormat, dtFormat);
			// 检查超长值
			_checkTooLongValue(value, fieldInfo, options);
			// 定长类型
			if (options.getIntItem("fileType") == 0) {
				int len = -1;
				try {
					len = fieldInfo.getIntItem("fieldLen")
							+ value.length()
							- value.getBytes(options.getStringItem("encoding",
									"UTF-8")).length;
				} catch (UnsupportedEncodingException e) {
					Logger.error("字符串转换为byte[]出错", value);
					e.printStackTrace();
				}
				if (!(len == -1)) {
					if (value.charAt(0) == '0') {
						value = Strutil.fillchar(value, len, ' ', 'l');
					}
				}
			}
		} else {
			throw Error.INPARAMS_INVALID;
		}
		// 返回结果
		return value;
	}

	// D1-日期类型(YYYY-MM-DD)
	public static String method_D1(int methodType, String value,
			JavaDict fieldInfo, JavaDict options) throws Error {
		try {
			return _method_DateTime(methodType, value, fieldInfo, options,
					"yyyy-MM-dd", null);
		} catch (Error e) {
			Logger.error("日期类型D1处理出错");
			throw e;
		}
	}

	// D2-日期类型(YYYYMMDD)
	public static String method_D2(int methodType, String value,
			JavaDict fieldInfo, JavaDict options) throws Error {
		try {
			return _method_DateTime(methodType, value, fieldInfo, options,
					"yyyyMMdd", null);
		} catch (Error e) {
			Logger.error("日期类型D2处理出错");
			throw e;
		}
	}

	// T1-时间类型(hh:mm:ss)
	public static String method_T1(int methodType, String value,
			JavaDict fieldInfo, JavaDict options) throws Error {
		try {
			return _method_DateTime(methodType, value, fieldInfo, options,
					"hh:mm:ss", "hhmmss");
		} catch (Error e) {
			Logger.error("时间类型T1处理出错");
			throw e;
		}
	}

	// T2-时间类型(hh-mm-ss)
	public static String method_T2(int methodType, String value,
			JavaDict fieldInfo, JavaDict options) throws Error {
		try {
			return _method_DateTime(methodType, value, fieldInfo, options,
					"hh-mm-ss", "hhmmss");
		} catch (Error e) {
			Logger.error("时间类型T2处理出错");
			throw e;
		}
	}

	public JavaDict getRunTimeInfo() {
		return runTimeInfo;
	}
	
	//信用卡对账文件剥离
	public JavaDict xykFile(String fileName, String filePath, String xykFilePath) throws Exception {
		//fileName B201801100016_77_01.txt
		//filePath /home/afa4j/fdirbak/EPCC/20180110/
		//xykFilePath /home/afa4j/fdir/EPCC/sendylfile/20180110/
		
		// 剥离后存放文件路径和文件名容器
		JavaDict readResult = null;
//			StringBuffer sb = new StringBuffer("");
//		FileReader reader = new FileReader("/home/afa4j/fdirbak/EPCC/20180110/B201801100016_77_01.txt");
		//原文件路径及名称
		String fullFileName = filePath + fileName ; 
		BufferedReader br = new BufferedReader(new InputStreamReader(
				new FileInputStream(fullFileName), "GBK"));
		
		//信用卡对账文件
		String xykFileFullName = xykFilePath + fileName;
		//"/home/afa4j/fdirbak/EPCC/20180110/XYK/B201801100016_77_01.txt";
		File newFile = new File(xykFileFullName);
		boolean success;
		if (!newFile.exists()){
			Logger.info("文件不存在");
			Logger.info(xykFileFullName);
			success = newFile.createNewFile();
		}else {
			Logger.info("文件存在,先删除");
//				newFile.delete();
//				success = newFile.createNewFile();
		}
//			Logger.info(success);
//		BufferedWriter writer = new BufferedWriter(
//				new OutputStreamWriter(new FileOutputStream(newFile), "GBK"));
		Logger.info("文件写入对象");
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(newFile, true), "GBK"));
		
		try {
			String line = null;
			int lineNum = 0;//行数
			int mxTotal = 0;//明细总数
			int xykNum = 0;//信用卡明细数
			BigDecimal xykTotalAmount = new BigDecimal("0");//初始化信用卡

			//初始化存放行数容器
			JavaDict lineDict = new JavaDict(); 
			
			Logger.info("开始读取文件");
			while ((line = br.readLine()) != null){
				lineNum++;
				if (lineNum == 1){
					//第一行特殊处理
					mxTotal = Integer.parseInt((line.split("\\|")[0])); 
					Logger.info("明细总数量:" + mxTotal);
				}else if (lineNum < mxTotal + 2){
					//明细行
					Logger.info("读取行数：" + lineNum);
					Logger.info(line);
					String[] tmp = line.split("\\|");
					if ("C1082744000038".equals(tmp[8]) && ("01".equals(tmp[1]) || "02".equals(tmp[1]))){
//						System.out.println(tmp[4]);
						//存放在一个javalist中  后续循环去写入文件
						Logger.info("付款信用卡");
						xykTotalAmount = xykTotalAmount.add(new BigDecimal(tmp[4].substring(3, tmp[4].length())));
						xykNum++;
						Logger.info("金额计算成功，数量计算成功");
						lineDict.setItem(xykNum + 1, line);
					}else if ("C1082744000038".equals(tmp[11]) && ("01".equals(tmp[2]) || "02".equals(tmp[2]))){
						Logger.info("收款款信用卡");
						xykTotalAmount = xykTotalAmount.add(new BigDecimal(tmp[4].substring(3, tmp[4].length())));
						xykNum++;
						lineDict.setItem(xykNum + 1, line);
					}
				}
				
			}
			
			
			if (xykNum == 0){
				//如果信用卡笔数为0，删除该文件
				newFile.delete();
			}else{
				//第一行内容
				lineDict.setItem(1, xykNum + "|" + xykTotalAmount);
				//最后一行内容
				lineDict.setItem(xykNum + 2, "<end>");
				
				//循环写入文件中
				for (int i = 1; i <= xykNum + 2; i ++){
					writer.write(lineDict.getStringItem(i) + "\n");
				}
			}
			
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally{
			writer.flush();
			writer.close();
			br.close();
		}
		
		return readResult;
		
	}
	
}

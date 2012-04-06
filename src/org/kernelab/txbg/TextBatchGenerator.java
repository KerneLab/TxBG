package org.kernelab.txbg;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.kernelab.basis.JSON;
import org.kernelab.basis.TextFiller;
import org.kernelab.basis.Tools;
import org.kernelab.basis.JSON.JSAN;

public class TextBatchGenerator implements Runnable
{
	public static String		DEFAULT_CHARSET_NAME	= "GBK";

	protected static final int	LOGICAL_NOT				= -1;

	protected static final int	LOGICAL_OR				= 0;

	protected static final int	LOGICAL_AND				= 1;

	protected static final int	DEFAULT_LOGIC			= LOGICAL_OR;

	protected static String		LAST_DIR				= ".";

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{

	}

	public static boolean Satisfies(JSON tags, JSAN cnds)
	{
		boolean result = true;

		if (tags != null && cnds != null && !cnds.isEmpty()) {

			result = false;

			JSON cnd = null;
			JSAN nst = null;
			Object t = null;
			Object c = null;

			int logic = DEFAULT_LOGIC;

			for (Object o : cnds) {

				if (JSON.IsJSON(o)) {

					if (result && logic == DEFAULT_LOGIC) {
						break;
					}

					boolean present = false;

					if ((nst = JSON.AsJSAN(o)) != null) {

						present = Satisfies(tags, nst);

					} else if ((cnd = JSON.AsJSON(o)) != null) {

						present = true;

						for (String k : cnd.keySet()) {

							t = tags.attr(k);
							c = cnd.attr(k);

							if ((t == null && c != null) || (t != null && c == null)) {
								present = false;
							} else if (t != null && c != null) {
								present = t.toString().matches(c.toString());
							}

							if (!present) {
								break;
							}
						}
					}

					switch (logic)
					{
						case LOGICAL_NOT:
							result = !present;
							break;

						case LOGICAL_OR:
							result = result || present;
							break;

						case LOGICAL_AND:
							result = result && present;
							break;
					}

					logic = DEFAULT_LOGIC;

				} else if (o != null) {
					String op = o.toString();
					if ("!".equals(op)) {
						logic = LOGICAL_NOT;
					} else if ("|".equals(op)) {
						logic = LOGICAL_OR;
					} else if ("&".equals(op)) {
						logic = LOGICAL_AND;
					}
				}
			}
		}

		return result;
	}

	private TextBatchGeneratorGUI	gui			= null;

	private TextFiller				filler		= new TextFiller();

	private JSON.Context			jsons		= new JSON.Context();

	private List<File>				list		= new LinkedList<File>();

	private boolean					chain;

	private boolean					generating	= false;

	public void acceptDataFile(File data)
	{
		if (data != null) {
			acceptDataFiles(Tools.listOfArray(new LinkedList<File>(), new File[] { data }), false);
		}
	}

	public void acceptDataFiles(List<File> list, boolean chain)
	{
		if (!generating()) {
			generating(true);
			this.list.clear();
			this.list.addAll(list);
			chain(chain);
			new Thread(this).start();
		}
	}

	protected void accumulate(JSON tags, JSAN aqm, JSAN aqms)
	{
		JSON ao = null;
		JSAN ac = null;
		boolean as = true;
		JSAN aqmp = null;

		TextFiller f = new TextFiller();

		int i = 0;
		int j = 0;

		for (Object o : aqm) {

			if (i++ % 2 == 0) {

				if ((aqmp = JSON.AsJSAN(o)) != null && !aqmp.isEmpty()) {

					for (Object oa : aqmp) {

						if ((ac = JSON.AsJSAN(oa)) != null) {
							as = Satisfies(tags, ac);
						} else if ((ao = JSON.AsJSON(oa)) != null) {

							if (as) {

								JSON aq = aqms.attrJSON(j);

								for (Entry<String, Object> entry : ao.entrySet()) {
									String key = entry.getKey();
									Object template = entry.getValue();
									String result = null;
									if (template != null) {
										result = (aq.get(key) == null ? "" : aq.get(key))
												+ f.reset(template.toString()).fillWith(tags).toString();
									}
									aq.attr(key, result);
								}
							}

							j++;
						}
					}
				}
			}
		}
	}

	public boolean chain()
	{
		return chain;
	}

	protected TextBatchGenerator chain(boolean chain)
	{
		this.chain = chain;
		return this;
	}

	public File chooseResultFile()
	{
		JFileChooser fc = new JFileChooser(LAST_DIR);

		fc.setDialogTitle("将生成结果文件保存为...");

		File result = null;

		s: while (fc.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
			if (fc.getSelectedFile().exists()) {
				switch (JOptionPane.showConfirmDialog(gui, "文件已存在，是否覆盖？", "保存目标冲突", JOptionPane.YES_NO_CANCEL_OPTION))
				{
					case JOptionPane.CANCEL_OPTION:
						result = null;
						break s;
					case JOptionPane.YES_OPTION:
						result = fc.getSelectedFile();
						break s;
				}
			} else {
				result = fc.getSelectedFile();
				break;
			}
		}

		return result;
	}

	protected void fillTemplate(String tmp, JSON tags)
	{
		Tools.debug(filler.reset(tmp).fillWith(tags).toString());
	}

	protected void fillTemplates(JSAN tmps, JSON tags)
	{
		if (tmps != null && !tmps.isEmpty()) {

			JSAN tc = null;
			JSON tt = null;
			boolean ts = true;

			for (Object t : tmps) {
				if ((tc = JSON.AsJSAN(t)) != null) {
					ts = Satisfies(tags, tc);
				} else if (ts && (tt = JSON.AsJSON(t)) != null) {
					tags.putAll(tt);
				} else if (ts && t != null) {
					fillTemplate(t.toString(), tags);
				}
			}
		}
	}

	protected JSAN filterTagList(JSAN tags, JSON tag)
	{
		JSAN result = new JSAN();

		if (tags != null && !tags.isEmpty()) {

			JSON t = null;
			JSAN tc = null;
			boolean ts = true;

			for (Object ot : tags) {

				if ((tc = JSON.AsJSAN(ot)) != null) {
					ts = Satisfies(tag, tc);
				} else if (ts && (t = JSON.AsJSON(ot)) != null) {
					result.add(t);
				}
			}
		}

		return result;
	}

	public void generate(JSON json, JSON tags)
	{
		JSAN tag = json.attrJSAN("tag");
		JSAN aqm = json.attrJSAN("aqm");
		JSAN tmp = json.attrJSAN("tmp");
		JSAN sub = json.attrJSAN("sub");

		if (tag != null && !tag.isEmpty()) {

			JSON t = null;

			if (aqm != null && !aqm.isEmpty()) {

				JSAN aqms = new JSAN();

				JSAN aqmp = null;

				int i = 0;
				for (Object oa : aqm) {
					if (i++ % 2 == 0 && (aqmp = JSON.AsJSAN(oa)) != null) {
						JSON ao = null;
						for (Object o : aqmp) {
							if (!JSON.IsJSAN(o) && (ao = JSON.AsJSON(o)) != null) {
								JSON a = new JSON();
								for (String k : ao.keySet()) {
									a.put(k, null);
								}
								aqms.add(a);
							}
						}
					}
				}

				JSON temp = null;

				for (Object ot : tag) {
					if (!JSON.IsJSAN(ot) && (t = JSON.AsJSON(ot)) != null) {
						temp = tags.clone();
						temp.putAll(t);
						accumulate(temp, aqm, aqms);
					}
				}

				i = 0;
				int j = 0;
				for (Object oa : aqm) {

					aqmp = JSON.AsJSAN(oa);

					switch (i++ % 2)
					{
						case 0:
							if (aqmp != null && !aqmp.isEmpty()) {

								JSON ao = null;
								JSON at = new JSON();

								for (Object o : aqmp) {

									if (!JSON.IsJSAN(o) && (ao = JSON.AsJSON(o)) != null) {

										JSON aq = aqms.attrJSON(j++);

										for (Entry<String, Object> entry : aq.entrySet()) {
											String k = entry.getKey();
											Object v = entry.getValue();
											if (v != null) {
												v = (at.get(k) == null ? "" : at.get(k)) + v.toString();
											}
											if (v != null || ao.get(k) == null) {
												at.put(k, v);
											}
										}
									}
								}

								tags.putAll(at);
							}
							break;

						case 1:
							fillTemplates(aqmp, tags.clone());
							break;
					}
				}
			}

			JSAN tc = null;
			boolean ts = true;

			for (Object ot : tag) {

				if ((tc = JSON.AsJSAN(ot)) != null) {
					ts = Satisfies(tags, tc);
				} else if (ts && (t = JSON.AsJSON(ot)) != null) {

					t = t.clone();

					for (Entry<String, Object> entry : t.entrySet()) {
						if (JSON.IsJSAN(entry.getValue())) {
							t.put(entry.getKey(), filterTagList((JSAN) entry.getValue(), tags));
						}
					}

					JSON temp = tags.clone();
					temp.putAll(t);

					fillTemplates(tmp, temp);

					if (sub != null && !sub.isEmpty()) {

						JSON s = null;
						JSAN sc = null;
						boolean ss = true;

						for (Object os : sub) {

							temp = tags.clone();
							temp.putAll(t);

							if ((sc = JSON.AsJSAN(os)) != null) {
								ss = Satisfies(temp, sc);
							} else if (ss && (s = JSON.AsJSON(os)) != null) {
								generate(s, temp);
							}
						}
					}
				}
			}
		} else {
			fillTemplates(tmp, tags);
		}
	}

	public boolean generating()
	{
		return generating;
	}

	protected TextBatchGenerator generating(boolean generating)
	{
		this.generating = generating;
		if (gui != null) {
			gui.processing(generating);
		}
		return this;
	}

	public TextBatchGeneratorGUI gui()
	{
		return gui;
	}

	public TextBatchGenerator gui(TextBatchGeneratorGUI gui)
	{
		this.gui = gui;
		return this;
	}

	public void process(File data)
	{
		if (!chain) {
			jsons.clear();
		}
		jsons.read(data, DEFAULT_CHARSET_NAME);
		generate(jsons.attrJSON("main"), new JSON().outer(jsons));
	}

	public void run()
	{
		if (list != null && list.size() > 0) {

			for (File file : list) {
				LAST_DIR = file.getParent();
				break;
			}

			File result = null;
			PrintStream ps = null;

			do {
				result = chooseResultFile();

				if (result == null) {
					break;
				}

				boolean legal = true;

				if (legal) {
					String resultFilePath = result.getAbsolutePath();
					for (File file : list) {
						if (file.getAbsolutePath().equals(resultFilePath)) {
							legal = false;
							JOptionPane.showMessageDialog(gui, "生成结果文件不能覆盖模板数据文件，请重新选择！", "错误",
									JOptionPane.ERROR_MESSAGE);
							break;
						}
					}
				}

				if (legal) {
					try {
						ps = new PrintStream(result, DEFAULT_CHARSET_NAME);
					} catch (FileNotFoundException e) {
						legal = false;
						JOptionPane.showMessageDialog(gui, "目标生成文件暂不可写入，请检查该文件是否被其他程序占用！", "警告",
								JOptionPane.ERROR_MESSAGE);
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
				}

				if (legal) {
					break;
				}
			} while (true);

			if (result != null && ps != null) {

				LAST_DIR = result.getParent();

				for (File data : list) {

					Tools.resetOuts();
					Tools.debug(data.getAbsolutePath());
					Tools.getOuts().add(ps);

					try {

						process(data);

					} catch (RuntimeException e) {
						if (JOptionPane.showConfirmDialog(gui, "不能正确解析模板数据文件：\n" + data.getAbsolutePath()
								+ "\n是否继续处理剩余的文件？", "错误", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.NO_OPTION)
						{
							break;
						}
					}
				}

				ps.close();

				Tools.resetOuts();
				Tools.debug(result.getAbsolutePath());

				JOptionPane.showMessageDialog(gui, "文本填充结果保存在\n" + result.getAbsolutePath(), "完成",
						JOptionPane.INFORMATION_MESSAGE);
			}
		}
		generating(false);
	}
}

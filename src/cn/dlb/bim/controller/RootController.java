package cn.dlb.bim.controller;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.node.ObjectNode;

import cn.dlb.bim.component.PlatformInitDatas;
import cn.dlb.bim.component.PlatformServer;
import cn.dlb.bim.dao.entity.Project;
import cn.dlb.bim.ifc.collada.KmzSerializer;
import cn.dlb.bim.ifc.database.IfcModelDbException;
import cn.dlb.bim.ifc.database.IfcModelDbSession;
import cn.dlb.bim.ifc.database.OldQuery;
import cn.dlb.bim.ifc.database.queries.om.JsonQueryObjectModelConverter;
import cn.dlb.bim.ifc.database.queries.om.Query;
import cn.dlb.bim.ifc.database.queries.om.QueryException;
import cn.dlb.bim.ifc.emf.IfcModelInterface;
import cn.dlb.bim.ifc.emf.IfcModelInterfaceException;
import cn.dlb.bim.ifc.emf.PackageMetaData;
import cn.dlb.bim.ifc.emf.ProjectInfo;
import cn.dlb.bim.ifc.emf.Schema;
import cn.dlb.bim.ifc.engine.cells.Vector3d;
import cn.dlb.bim.ifc.model.BasicIfcModel;
import cn.dlb.bim.ifc.serializers.SerializerException;
import cn.dlb.bim.ifc.tree.ProjectTree;
import cn.dlb.bim.service.BimService;
import cn.dlb.bim.service.ProjectService;
import cn.dlb.bim.vo.GlbVo;

@Controller
@RequestMapping("/")
public class RootController {

	private static final Logger LOGGER = LoggerFactory.getLogger(RootController.class);

	@Autowired
	@Qualifier("BimServiceImpl")
	private BimService bimService;
	
	@Autowired
	@Qualifier("ProjectServiceImpl")
	private ProjectService projectService;
	
	@Autowired
	@Qualifier("PlatformServer")
	private PlatformServer server;

	@RequestMapping("index")
	public String index() {
		return "index";
	}

	// 前端路由专用页面
	@RequestMapping("app")
	public String app() {
		return "app/index.jsp";
	}

	@RequestMapping(value = "queryGeometryInfo", method = RequestMethod.GET)
	@ResponseBody
	public Map<String, Object> queryGeometryInfo(@RequestParam("rid") Integer rid) {
		Map<String, Object> resMap = new HashMap<String, Object>();
		resMap.put("success", true);
		resMap.put("geometries", bimService.queryGeometryInfo(rid));
		return resMap;
	}

	@RequestMapping(value = "addRevision", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> addRevision(@RequestParam("pid") Long pid, @RequestParam(value = "file", required = true) MultipartFile file,
			HttpServletRequest request// , ModelMap model
	) {
		Map<String, Object> resMap = new HashMap<String, Object>();
		
		Project project = projectService.queryProject(pid);
		if (project == null) {
			resMap.put("error", true);
			resMap.put("msg", "project with pid = " + pid + " is null");
			return resMap;
		}
		String path = request.getSession().getServletContext().getRealPath("upload/ifc/");
		String fileName = file.getOriginalFilename();
		String[] split = fileName.split("\\.");
		String suffix = null;
		if (split.length >= 2) {
			suffix = split[split.length - 1];
		}
		if (suffix == null || !suffix.equals("ifc")) {
			resMap.put("error", true);
			resMap.put("msg", "suffix : " + suffix + " is not be supported");
			return resMap;
		} 
		String newFileName = fileName.substring(0, fileName.lastIndexOf("."));
		newFileName += "-" + System.currentTimeMillis();
		newFileName += "." + suffix;
		File targetFile = new File(path, newFileName);
		if (!targetFile.exists()) {
			targetFile.mkdirs();
		}
		try {
			file.transferTo(targetFile);
		} catch (Exception e) {
			e.printStackTrace();
		}
		int rid = bimService.addRevision(pid, targetFile);
		resMap.put("success", "true");
		resMap.put("rid", rid);
		return resMap;
	}
	
	@RequestMapping(value = "jsonApi", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> jsonApi(@RequestBody ObjectNode jsonNode) {//DEMO
		PackageMetaData packageMetaData = server.getMetaDataManager()
				.getPackageMetaData(Schema.IFC2X3TC1.getEPackageName());
		JsonQueryObjectModelConverter converter = new JsonQueryObjectModelConverter(packageMetaData);
		Query query = null;
		try {
			query = converter.parseJson("query", jsonNode);
		} catch (QueryException e) {
			e.printStackTrace();
		}
		Map<String, Object> resMap = new HashMap<String, Object>();
		return resMap;
	}
	
	@RequestMapping(value = "queryGlbByRid", method = RequestMethod.GET)
	public void queryGlbByRid(@RequestParam("rid")Integer rid, HttpServletResponse response) {
		GlbVo glbVo = bimService.queryGlbByRid(rid);
		try {
			OutputStream os = response.getOutputStream();
			os.write(glbVo.getData());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@RequestMapping(value = "queryGlbLonlatByRid", method = RequestMethod.GET)
	@ResponseBody
	public Vector3d queryGlbLonlatByRid(@RequestParam("rid")Integer rid) {
		return bimService.queryGlbLonlatByRid(rid);
	}
	
	@RequestMapping(value = "kml", method = RequestMethod.GET)
	public void kml(@RequestParam("rid")Integer rid) {
		IfcModelInterface model = bimService.queryModelByRid(rid);
		KmzSerializer serializer = new KmzSerializer();
		ProjectInfo projectInfo = new ProjectInfo();
		projectInfo.setName("bim");
		projectInfo.setAuthorName("linfujun");
		try {
			serializer.init(model, projectInfo, true);
			serializer.writeToFile(new File("test.kmz").toPath(), null);
		} catch (SerializerException e) {
			e.printStackTrace();
		}
	}
	
	@RequestMapping(value = "queryModelTree", method = RequestMethod.GET)
	public ProjectTree queryModelTree(@RequestParam("rid")Integer rid) {
		IfcModelInterface model = bimService.queryModelByRid(rid);
		ProjectTree tree = new ProjectTree(model.getPackageMetaData());
		tree.buildProjectTree(model);
		return tree;
	}
	
}
package cn.dlb.bim.controller;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.emf.ecore.EClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import cn.dlb.bim.dao.entity.Project;
import cn.dlb.bim.ifc.collada.KmzSerializer;
import cn.dlb.bim.ifc.emf.IdEObject;
import cn.dlb.bim.ifc.emf.IfcModelInterface;
import cn.dlb.bim.ifc.emf.ProjectInfo;
import cn.dlb.bim.ifc.engine.cells.Vector3d;
import cn.dlb.bim.ifc.serializers.SerializerException;
import cn.dlb.bim.ifc.tree.Material;
import cn.dlb.bim.ifc.tree.MaterialGenerator;
import cn.dlb.bim.ifc.tree.ProjectTreeGenerator;
import cn.dlb.bim.ifc.tree.PropertyGenerator;
import cn.dlb.bim.models.ifc2x3tc1.IfcProduct;
import cn.dlb.bim.service.BimService;
import cn.dlb.bim.service.ProjectService;
import cn.dlb.bim.vo.GlbVo;
import cn.dlb.bim.vo.ModelInfoVo;
import cn.dlb.bim.web.ResultUtil;

@Controller
@RequestMapping("/model/")
public class ModelController {
	
	@Autowired
	@Qualifier("BimServiceImpl")
	private BimService bimService;
	
	@Autowired
	@Qualifier("ProjectServiceImpl")
	private ProjectService projectService;
	
	@RequestMapping(value = "addModel", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> addModel(ModelInfoVo modelInfo, @RequestParam(value = "file", required = true) MultipartFile file,
			HttpServletRequest request// , ModelMap model
	) {
		ResultUtil result = new ResultUtil();
		Long pid = modelInfo.getPid();
		Project project = projectService.queryProject(pid);
		if (project == null) {
			result.setSuccess(false);
			result.setMsg("project with pid = " + pid + " is null");
			return result.getResult();
		}
		String path = request.getSession().getServletContext().getRealPath("upload/ifc/");
		String fileName = file.getOriginalFilename();
		String[] split = fileName.split("\\.");
		String suffix = null;
		if (split.length >= 2) {
			suffix = split[split.length - 1];
		}
		if (suffix == null || !suffix.equals("ifc")) {
			result.setSuccess(false);
			result.setMsg("suffix : " + suffix + " is not be supported");
			return result.getResult();
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
		int rid = bimService.addRevision(modelInfo, targetFile);
		result.setSuccess(true);
		result.setKeyValue("rid", rid);
		return result.getResult();
	}
	
	@RequestMapping(value = "queryModelInfoByPid", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> queryModelInfoByPid(@RequestParam("pid") Long pid) {
		ResultUtil result = new ResultUtil();
		List<ModelInfoVo> modelInfoList = bimService.queryModelInfoByPid(pid);
		result.setSuccess(true);
		result.setData(modelInfoList);
		return result.getResult();
	}
	
	@RequestMapping(value = "deleteModel", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> deleteModel(@RequestParam("rid") Integer rid) {
		ResultUtil result = new ResultUtil();
		bimService.deleteModel(rid);
		return result.getResult();
	}
	
	@RequestMapping(value = "queryGeometryInfo", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> queryGeometryInfo(@RequestParam("rid") Integer rid) {
		ResultUtil result = new ResultUtil();
		result.setSuccess(true);
		result.setKeyValue("geometries", bimService.queryGeometryInfo(rid));
		return result.getResult();
	}
	
	@RequestMapping(value = "queryModelProjectTree", method = RequestMethod.GET)
	@ResponseBody
	public Map<String, Object> queryModelTree(@RequestParam("rid")Integer rid) {
		ResultUtil result = new ResultUtil();
		IfcModelInterface model = bimService.queryModelByRid(rid);
		ProjectTreeGenerator treeGenerator = new ProjectTreeGenerator(model.getPackageMetaData());
		treeGenerator.buildProjectTree(model, ProjectTreeGenerator.KeyWord_IfcProject);
		result.setSuccess(true);
		result.setData(treeGenerator.getTree());
		return result.getResult();
	}
	
	@RequestMapping(value = "queryModelBuildingStoreyTree", method = RequestMethod.GET)
	@ResponseBody
	public Map<String, Object> queryModelBuildingStoreyTree(@RequestParam("rid")Integer rid) {
		ResultUtil result = new ResultUtil();
		IfcModelInterface model = bimService.queryModelByRid(rid);
		ProjectTreeGenerator treeGenerator = new ProjectTreeGenerator(model.getPackageMetaData());
		treeGenerator.buildProjectTree(model, ProjectTreeGenerator.KeyWord_IfcBuildingStorey);
		result.setSuccess(true);
		result.setData(treeGenerator.getTree());
		return result.getResult();
	}
	
	@RequestMapping(value = "queryProperty", method = RequestMethod.GET)
	@ResponseBody
	public Map<String, Object> queryProperty(@RequestParam("rid")Integer rid//, @RequestParam("oid")Long oid
			) {
		ResultUtil result = new ResultUtil();
		IfcModelInterface model = bimService.queryModelByRid(rid);
		EClass productClass = (EClass) model.getPackageMetaData().getEClassifierCaseInsensitive("IfcProduct");
		List<IdEObject> projectList = model.getAllWithSubTypes(productClass);
		
		for (IdEObject ifcProject : projectList) {
			PropertyGenerator p = new PropertyGenerator();
			p.getProperty(model.getPackageMetaData(), ifcProject);
		}
		result.setSuccess(true);
		return result.getResult();
	}
	
	@RequestMapping(value = "queryMaterial", method = RequestMethod.GET)
	@ResponseBody
	public Map<String, Object> queryMaterial(@RequestParam("rid")Integer rid//, @RequestParam("oid")Long oid
			) {
		ResultUtil result = new ResultUtil();
		IfcModelInterface model = bimService.queryModelByRid(rid);
		EClass productClass = (EClass) model.getPackageMetaData().getEClassifierCaseInsensitive("IfcProduct");
		List<IdEObject> projectList = model.getAllWithSubTypes(productClass);
		
		MaterialGenerator materialGetter = new MaterialGenerator(model);
		for (IdEObject ifcProject : projectList) {
			Material material = materialGetter.getMaterial(((IfcProduct) ifcProject));
			if (material != null) {
				System.out.println("type : " + ifcProject.eClass().getName() 
						+ " color : r " + material.getAmbient().r
						+ " g " + material.getAmbient().g
						+ " b " + material.getAmbient().b
						+ " a " + material.getTransparency());
			} else {
				System.out.println("type : " + ifcProject.eClass().getName() + "no material.");
			}
			
		}
		result.setSuccess(true);
		return result.getResult();
	}
	
	@RequestMapping(value = "queryGlbByRid", method = RequestMethod.POST)
	public void queryGlbByRid(@RequestParam("rid")Integer rid, HttpServletResponse response) {
		GlbVo glbVo = bimService.queryGlbByRid(rid);
		try {
			OutputStream os = response.getOutputStream();
			os.write(glbVo.getData());
			os.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@RequestMapping(value = "queryGlbLonlatByRid", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> queryGlbLonlatByRid(@RequestParam("rid")Integer rid) {
		ResultUtil result = new ResultUtil();
		
		Vector3d lonlat = bimService.queryGlbLonlatByRid(rid);
		
		if (lonlat == null) {
			result.setSuccess(false);
			result.setMsg("no glb finded by rid");
		} else {
			result.setSuccess(true);
			result.setData(lonlat);
		}
		
		return result.getResult();
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
}

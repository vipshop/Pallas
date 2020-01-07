/**
 * Copyright 2019 vip.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package com.vip.pallas.console.controller.index.version;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.Min;

import com.vip.pallas.console.vo.IndexVersionDynamicVO;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.vip.pallas.bean.IndexOperationEventName;
import com.vip.pallas.bean.IndexOperationEventType;
import com.vip.pallas.console.utils.AuditLogUtil;
import com.vip.pallas.console.utils.AuthorizeUtil;
import com.vip.pallas.console.utils.SessionUtil;
import com.vip.pallas.console.vo.IndexVersionVO;
import com.vip.pallas.console.vo.PageResultVO;
import com.vip.pallas.console.vo.base.BaseIndexVersionOp;
import com.vip.pallas.exception.BusinessLevelException;
import com.vip.pallas.exception.PallasException;
import com.vip.pallas.mybatis.entity.Cluster;
import com.vip.pallas.mybatis.entity.Index;
import com.vip.pallas.mybatis.entity.IndexOperation;
import com.vip.pallas.mybatis.entity.IndexVersion;
import com.vip.pallas.mybatis.entity.Page;
import com.vip.pallas.service.ClusterService;
import com.vip.pallas.service.ElasticSearchService;
import com.vip.pallas.service.IndexOperationService;
import com.vip.pallas.service.IndexService;
import com.vip.pallas.service.IndexVersionService;
import com.vip.pallas.utils.JsonUtil;
import com.vip.pallas.utils.ObjectMapTool;

@Validated
@RestController
@RequestMapping("/index/version")
public class IndexVersionController {

	private static Logger logger = LoggerFactory.getLogger(IndexVersionController.class);
	@Autowired
	private IndexVersionService indexVersionService;
	@Autowired
	private IndexOperationService indexOperationService;
	@Autowired
	private IndexService indexService;
	@Autowired
	private ElasticSearchService elasticSearchService;
	@Autowired
	private ClusterService clusterService;

	@RequestMapping(value = "/page.json", method = RequestMethod.GET)
	public PageResultVO<IndexVersion> page(@RequestParam Long indexId,
			@RequestParam(required = false, defaultValue = "1") @Min(value = 0, message = "currentPage必须为正数") Integer currentPage,
			@RequestParam(required = false, defaultValue = "10") @Min(value = 0, message = "pageSize必须为正数") Integer pageSize) { // NOSONAR

		Page<IndexVersion> page = new Page<>();
		page.setPageNo(currentPage);
		page.setPageSize(pageSize);

		PageResultVO<IndexVersion> resultVO = new PageResultVO<>();

		resultVO.setList(indexVersionService.findPage(page, indexId));
		resultVO.setTotal(page.getTotalRecord());
		resultVO.setPageCount(page.getTotalPage());


		List<String> privileges = AuthorizeUtil.loadPrivileges();
		if (privileges != null
				&& (privileges.contains("version." + indexId + "-" + indexService.findById(indexId).getIndexName())
						|| privileges.contains("version.all"))) {
			resultVO.setAllPrivilege(true);
		}

		return resultVO;
	}

	@RequestMapping(value = "/count.json", method = RequestMethod.POST)
	public List<Map<String, Object>> countDocuments(@RequestBody Map<String, Object> params) { // NOSONAR
		List<Long> versionIds = ObjectMapTool.getLongList(params, "versionIds");
		String indexName = ObjectMapTool.getString(params, "indexName");
		Long indexId = ObjectMapTool.getLong(params, "indexId");

		if (ObjectUtils.isEmpty(versionIds)) {
			throw new BusinessLevelException(500, "versionIds不能为空");
		}

		if (ObjectUtils.isEmpty(indexName)) {
			throw new BusinessLevelException(500, "indexName不能为空");
		}

		if (ObjectUtils.isEmpty(indexId)) {
			throw new BusinessLevelException(500, "indexId不能为空");
		}

		List<Map<String, Object>> versionCountList = new ArrayList<>();
		for (Long vid : versionIds) {
			try {
				com.vip.pallas.bean.IndexVersion iv = indexVersionService.findVersionById(vid);
				if (iv != null) {
					List<Map> oneVersionCount = new ArrayList<>();
					List<Cluster> clusters = clusterService.selectPhysicalClustersByIndexId(indexId);
					for (Cluster cluster : clusters) {
						Long dataCount = elasticSearchService.getDataCount(indexName, cluster.getHttpAddress(), vid, cluster.getUsername(), cluster.getPasswd());
						Map<String, Long> countMap = new HashMap<>();
						countMap.put("cid", cluster.getId());
						countMap.put("count", dataCount == null ? 0L : dataCount);
						oneVersionCount.add(countMap);
					}
					Map<String, Object> result = new HashMap<>();
					result.put("vid", vid);
					result.put("data", oneVersionCount);
					versionCountList.add(result);
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
		return versionCountList;
	}

	@RequestMapping(value = "/id.json", method = RequestMethod.GET)
	public com.vip.pallas.bean.IndexVersion findById(@RequestParam Long versionId)
			throws SQLException, PallasException { // NOSONAR
		return indexVersionService.findVersionById(versionId);
	}

	@RequestMapping(value = "/copy.json")
	public com.vip.pallas.bean.IndexVersion copyById(@RequestBody @Validated BaseIndexVersionOp params)
			throws SQLException, PallasException {
		Long indexId = params.getIndexId();
		Long versionId = params.getVersionId();

		if (ObjectUtils.isEmpty(indexId)) {
			throw new BusinessLevelException(500, "indexId不能为空");
		}

		return indexVersionService.copyVersion(indexId, versionId);
	}

	@RequestMapping(value = "/info.json", method = RequestMethod.GET)
	public String info(@RequestParam Long versionId, @RequestParam Long cid, @RequestParam String indexName)
			throws Exception {
		try {
			Cluster cluster = clusterService.selectByPrimaryKey(cid);
			String indexInfo = elasticSearchService.getIndexInfo(indexName, cluster.getHttpAddress(), versionId, cluster.getUsername(), cluster.getPasswd());
			return indexInfo == null ? "该版本信息未在ES初始化,请先点击开始同步！" : indexInfo;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return null;
	}

	@RequestMapping(value = "/retrieve.json", method = RequestMethod.GET)
	public String retrieve(@RequestParam Long versionId, @RequestParam Long cid, @RequestParam String indexName) {
		try {
			Cluster cluster = clusterService.selectByPrimaryKey(cid);
			String indexInfo = elasticSearchService.retrieveIndex(indexName, cluster.getHttpAddress(), versionId, cluster.getUsername(), cluster.getPasswd());
			return indexInfo == null ? "该版本信息未在ES初始化,请先点击开始同步！" : indexInfo;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return null;
	}

	@RequestMapping(value = "/create_index.json")
	public void createIndex(@RequestBody @Validated BaseIndexVersionOp params, HttpServletRequest request) throws Exception {
		Long indexId = params.getIndexId();
		Long versionId = params.getVersionId();
		
		Index index = indexService.findById(indexId);
		if (ObjectUtils.isEmpty(index)) {
			throw new BusinessLevelException(500, "index不存在");
		}
		if (!AuthorizeUtil.authorizeIndexVersionPrivilege(request, indexId, index.getIndexName())) {
			throw new BusinessLevelException(403, "无权限操作");
		}
		
		IndexVersion v = indexVersionService.findById(versionId);
		if (v == null) {
			throw new BusinessLevelException(500, "versionId不存在");
		}

		elasticSearchService.createIndex(index.getIndexName(), indexId, versionId);

		indexVersionService.updateSyncState(versionId, true);
		try {
			AuditLogUtil.log("create index: id - {0}, name - {1}, versionId - {2}", v.getIndexId(),
					index.getIndexName(), versionId);

			IndexOperation record = new IndexOperation();
			record.setEventDetail("create index: id - " + v.getIndexId() + ", name - " + index.getIndexName()
					+ ",versionId - " + versionId);
			record.setEventName(IndexOperationEventName.BEGIN_SYN);
			record.setEventType(IndexOperationEventType.SYN_EVENT);
			record.setIndexId(index.getId());
			record.setOperationTime(new Date());
			record.setOperator(SessionUtil.getLoginUser(request));
			record.setVersionId(versionId);
			indexOperationService.insert(record);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	@RequestMapping(value = "/enable.json")
	public void enableVersion(@RequestBody @Validated BaseIndexVersionOp params, HttpServletRequest request) throws Exception {
		Long id = params.getVersionId();
		Long indexId = params.getIndexId();
		Index index = indexService.findById(indexId);
		if (index == null) {
			throw new BusinessLevelException(500, "该索引不存在");
		}
		
		if (!AuthorizeUtil.authorizeIndexVersionPrivilege(request, indexId, index.getIndexName())) {
			throw new BusinessLevelException(403, "无权限操作");
		} 
		
		IndexVersion v = indexVersionService.findById(id);
		if (v == null) {
			throw new BusinessLevelException(500, "versionId不存在");
		}
		indexVersionService.enableVersion(id);
		AuditLogUtil.log("enable versionId: id - {0},  indexId - {1}, indexName - {2}", id, index.getId(),
				index.getIndexName());
		try {
			IndexOperation record = new IndexOperation();
			record.setEventDetail(v.toString());
			record.setEventName(IndexOperationEventName.ENABLED_VERSION);
			record.setEventType(IndexOperationEventType.VERSION_EVENT);
			record.setIndexId(index.getId());
			record.setOperationTime(new Date());
			record.setOperator(SessionUtil.getLoginUser(request));
			record.setVersionId(Long.valueOf(id.toString()));
			indexOperationService.insert(record);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	@RequestMapping("/disable.json")
	public void stop(@RequestBody @Validated BaseIndexVersionOp params, HttpServletRequest request) throws Exception {
		Long indexId = params.getIndexId();
		Long versionId = params.getVersionId();
		
		Index index = indexService.findById(indexId);
		if (ObjectUtils.isEmpty(index)) {
			throw new BusinessLevelException(500, "index不存在");
		}
		if (!AuthorizeUtil.authorizeIndexVersionPrivilege(request, indexId, index.getIndexName())) {
			throw new BusinessLevelException(403, "无权限操作");
		}
		
		IndexVersion v = indexVersionService.findById(versionId);
		if (v == null) {
			throw new BusinessLevelException(500, "versionId不存在");
		}

		try {
			if (v.getIsUsed()) {
				indexVersionService.disableVersion(versionId);
			} else {
				indexVersionService.disableVersionSync(indexId, versionId);
			}
		} finally {
			try {
				AuditLogUtil.log("stop sync index: id - {0}, name - {1}, versionId - {2}", v.getIndexId(),
						index.getIndexName(), versionId);
				IndexOperation record = new IndexOperation();
				record.setEventDetail("stop sync index: id - " + v.getIndexId() + ", name - " + index.getIndexName()
						+ ", versionId - " + versionId);
				record.setEventName(IndexOperationEventName.STOP_SYN);
				record.setEventType(IndexOperationEventType.SYN_EVENT);
				record.setIndexId(index.getId());
				record.setOperationTime(new Date());
				record.setOperator(SessionUtil.getLoginUser(request));
				record.setVersionId(versionId);
				indexOperationService.insert(record);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
	}



	@RequestMapping("/delete/id.json")
	public void deleteById(@RequestBody @Validated BaseIndexVersionOp params, HttpServletRequest request) throws Exception {
		Long indexId = params.getIndexId();
		Long versionId = params.getVersionId();
		
		Index index = indexService.findById(indexId);
		if (ObjectUtils.isEmpty(index)) {
			throw new BusinessLevelException(500, "index不存在");
		}
		if (!AuthorizeUtil.authorizeIndexVersionPrivilege(request, indexId, index.getIndexName())) {
			throw new BusinessLevelException(403, "无权限操作");
		}

		IndexVersion v = indexVersionService.findById(versionId);
		if (v == null) {
			throw new BusinessLevelException(500, "versionId不存在");
		}
		indexVersionService.deleteVersion(versionId);

		try {
			AuditLogUtil.log("delete versionId: id - {0}, indexId - {1}, indexName - {2}", versionId, index.getId(),
					index.getIndexName());
			IndexOperation record = new IndexOperation();
			record.setEventDetail(v.toString());
			record.setEventName(IndexOperationEventName.DELETE_VERSION);
			record.setEventType(IndexOperationEventType.VERSION_EVENT);
			record.setIndexId(index.getId());
			record.setOperationTime(new Date());
			record.setOperator(SessionUtil.getLoginUser(request));
			record.setVersionId(Long.valueOf(versionId.toString()));
			indexOperationService.insert(record);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	@RequestMapping(value = "/add.json", method = RequestMethod.POST)
	public Long add(@RequestBody @Validated IndexVersionVO params, HttpServletRequest request) {
		Object schema = params.getSchema();
		params.setId(null);
		IndexVersion indexVersion = getIndexVersion(params);

		Index index = indexService.findById(indexVersion.getIndexId());
		if (index == null) {
			throw new BusinessLevelException(500, "该索引不存在");
		}
		
		if (!AuthorizeUtil.authorizeIndexVersionPrivilege(request, index.getId(), index.getIndexName())) {
			throw new BusinessLevelException(403, "无权限操作");
		}

		indexVersionService.insert(indexVersion, (ArrayList) schema);

		try {
			AuditLogUtil.log(
					"update versionId: id - {0}, index - {1}, numOfShards - {2}, numOfReplication - {3}, vdpQueue - {4}, routingKey - {5}, idField - {6}, updateTimeField - {7}",
					indexVersion.getId(), indexVersion.getIndexId(), indexVersion.getNumOfShards(),
					indexVersion.getNumOfReplication(), indexVersion.getVdpQueue(), indexVersion.getRoutingKey(),
					indexVersion.getIdField(), indexVersion.getUpdateTimeField());

			IndexOperation record = new IndexOperation();
			record.setEventDetail(indexVersion + "  " + schema);
			record.setEventName(IndexOperationEventName.CREATE_VERSION);
			record.setEventType(IndexOperationEventType.VERSION_EVENT);
			record.setIndexId(index.getId());
			record.setOperationTime(new Date());
			record.setOperator(SessionUtil.getLoginUser(request));
			record.setVersionId(indexVersion.getId());
			indexOperationService.insert(record);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return indexVersion.getId();
	}

	private IndexVersion getIndexVersion(IndexVersionVO params) {
		IndexVersion indexVersion = new IndexVersion();
		BeanUtils.copyProperties(params, indexVersion);
		indexVersion.setDynamic(Boolean.FALSE);

		return indexVersion;
	}

	@RequestMapping(value = "/update.json", method = RequestMethod.POST)
	public void update(@RequestBody @Validated IndexVersionVO params, HttpServletRequest request) throws Exception {
		Long versionId = params.getId();
		Object schema = params.getSchema();

		if (ObjectUtils.isEmpty(versionId)) {
			throw new BusinessLevelException(500, "versionId不能为空");
		}

		IndexVersion indexVersion = getIndexVersion(params);
		indexVersion.setId(versionId);

		Index index = indexService.findById(indexVersion.getIndexId());
		if (index == null) {
			throw new BusinessLevelException(500, "该索引不存在");
		}
		
		if (!AuthorizeUtil.authorizeIndexVersionPrivilege(request, index.getId(), index.getIndexName())) {
			throw new BusinessLevelException(403, "无权限操作");
		}

		indexVersionService.update(indexVersion, (ArrayList) schema);
		try {
			AuditLogUtil.log(
					"add versionId: id - {0}, index - {1}, numOfShards - {2}, numOfReplication - {3}, vdpQueue - {4}, routingKey - {5}, idField - {6}, updateTimeField - {7}",
					indexVersion.getId(), indexVersion.getIndexId(), indexVersion.getNumOfShards(),
					indexVersion.getNumOfReplication(), indexVersion.getVdpQueue(), indexVersion.getRoutingKey(),
					indexVersion.getIdField(), indexVersion.getUpdateTimeField());

			IndexOperation record = new IndexOperation();
			record.setEventDetail(indexVersion + "  " + schema);
			record.setEventName(IndexOperationEventName.UPDATE_VERSION);
			record.setEventType(IndexOperationEventType.VERSION_EVENT);
			record.setIndexId(index.getId());
			record.setOperationTime(new Date());
			record.setOperator(SessionUtil.getLoginUser(request));
			record.setVersionId(versionId);
			indexOperationService.insert(record);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	@RequestMapping(value = "/dynamic_update.json", method = RequestMethod.POST)
	public void update(@RequestBody @Validated IndexVersionDynamicVO params, HttpServletRequest request) throws Exception {
		Long versionId = params.getId();

		if (ObjectUtils.isEmpty(versionId)) {
			throw new BusinessLevelException(500, "versionId不能为空");
		}

		IndexVersion indexVersion = new IndexVersion();
		BeanUtils.copyProperties(params, indexVersion);
		indexVersion.setId(versionId);
		indexVersion.setUpdateTime(new Date());

		Index index = indexService.findById(indexVersion.getIndexId());
		if (index == null) {
			throw new BusinessLevelException(500, "该索引不存在");
		}

		if (!AuthorizeUtil.authorizeIndexVersionPrivilege(request, index.getId(), index.getIndexName())) {
			throw new BusinessLevelException(403, "无权限操作");
		}

		indexVersionService.update(indexVersion);
		elasticSearchService.dynamicUpdateIndexSettings(index.getIndexName(),index.getId(),indexVersion);
		try {
			AuditLogUtil.log(
					"add versionId: id - {0}, index - {1}, maxResultWindow - {2}, numOfReplication - {3}," +
							" flushThresholdSize - {4}, totalShardsPerNode - {5}, syncInterval - {6}," +
							" translogDurability - {7}, allocationNodes - {8} , indexSlowThreshold - {9}," +
							" fetchSlowThreshold - {10}, querySlowThreshold - {11}, refreshInterval - {12}",
					indexVersion.getId(), indexVersion.getIndexId(), indexVersion.getMaxResultWindow(),
					indexVersion.getNumOfReplication(), indexVersion.getFlushThresholdSize(), indexVersion.getTotalShardsPerNode(),
					indexVersion.getSyncInterval(), indexVersion.getTranslogDurability(),indexVersion.getAllocationNodes(),indexVersion.getIndexSlowThreshold(),
					indexVersion.getFetchSlowThreshold(),indexVersion.getQuerySlowThreshold(),indexVersion.getRefreshInterval());


			IndexOperation record = new IndexOperation();
			record.setEventDetail(indexVersion+"");
			record.setEventName(IndexOperationEventName.UPDATE_VERSION);
			record.setEventType(IndexOperationEventType.VERSION_EVENT);
			record.setIndexId(index.getId());
			record.setOperationTime(new Date());
			record.setOperator(SessionUtil.getLoginUser(request));
			record.setVersionId(versionId);
			indexOperationService.insert(record);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	@RequestMapping("/schema_import.json")
	public void importSchema(MultipartFile file, HttpServletResponse response, HttpServletRequest request)
			throws Exception {
		Long indexId = Long.valueOf((String) request.getParameter("indexId"));
		String jsonStr = IOUtils.toString(file.getInputStream(), StandardCharsets.UTF_8);

		if (!ObjectUtils.isEmpty(indexId)) {
			jsonStr = indexVersionService.importSchema(jsonStr, indexId);
		}
		response.setStatus(200);
		response.setContentType("application/json");
		response.getWriter().write(jsonStr);
		response.flushBuffer();
	}

	@RequestMapping("/schema_export.json")
	public void exportSchema(HttpServletResponse response, HttpServletRequest request) throws Exception {
		Long versionId = Long.valueOf((String) request.getParameter("versionId"));
		if (ObjectUtils.isEmpty(versionId)) {
			throw new BusinessLevelException(500, "versionId不能为空");
		}
		com.vip.pallas.bean.IndexVersion v = indexVersionService.findVersionById(versionId);
		Index index = indexService.findById(v.getIndexId());

		response.setStatus(200);
		response.setContentType("application/text");
		response.setHeader("Content-Disposition",
				"attachment; filename=\"schema_" + index.getIndexName() + "_" + v.getId() + "_.json\"");
		Map<String, Object> resultMap = new HashMap<String, Object>();
		resultMap.put("schema", indexVersionService.findVersionById(versionId).getSchema());
		response.getWriter().write(JsonUtil.toJson(resultMap));
		response.flushBuffer();
	}

}
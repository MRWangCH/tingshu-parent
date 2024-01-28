package com.atguigu.tingshu.album.service.impl;

import com.atguigu.tingshu.album.config.VodConstantProperties;
import com.atguigu.tingshu.album.mapper.TrackInfoMapper;
import com.atguigu.tingshu.album.service.TrackInfoService;
import com.atguigu.tingshu.common.util.UploadFileUtil;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qcloud.vod.VodUploadClient;
import com.qcloud.vod.model.VodUploadRequest;
import com.qcloud.vod.model.VodUploadResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class TrackInfoServiceImpl extends ServiceImpl<TrackInfoMapper, TrackInfo> implements TrackInfoService {

	@Autowired
	private TrackInfoMapper trackInfoMapper;

	@Autowired
	private VodConstantProperties props;

	/***
	 * 上传声音文件到腾讯云点播平台
	 * @param file 上传的声音文件
	 * @return
	 */
	@Override
	public Map<String, String> uploadTrack(MultipartFile file) {
		try {
			//将用户上传的临时文件保存到临时目录下
			String tempFilePath = UploadFileUtil.uploadTempPath(props.getTempPath(), file);
			//初始化一个上传客户端对象
			VodUploadClient client = new VodUploadClient(props.getSecretId(), props.getSecretKey());
			//构建上传请求对象
			VodUploadRequest request = new VodUploadRequest();
			request.setMediaFilePath(tempFilePath);
			VodUploadResponse response = client.upload(props.getRegion(), request);
			if (response!= null){
				//获取上传后文件地址
				String mediaUrl = response.getMediaUrl();
				//获取上传后文件唯一标识
				String fileId = response.getFileId();
				Map<String, String> map = new HashMap<>();
				map.put("mediaUrl", mediaUrl);
				map.put("mediaFileId", fileId);
				return map;
			}
			return null;
		} catch (Exception e) {
			log.error("云点播平台上传文件失败");
			throw new RuntimeException(e);
		}
	}
}

package com.window.service.impl;

import com.window.entity.SiteConfig;
import com.window.mapper.SiteConfigMapper;
import com.window.service.SiteConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class SiteConfigServiceImpl implements SiteConfigService {

    private final SiteConfigMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public SiteConfig get() {
        try {
            SiteConfig cfg = mapper.selectById(1);
            if (cfg == null) {
                cfg = new SiteConfig();
                cfg.setId(1);
                cfg.setConfigJson(defaultConfig());
                mapper.insert(cfg);
            }
            return cfg;
        } catch (Exception e) {
            // 表不存在或其他数据库异常时，返回内存中的默认配置
            SiteConfig cfg = new SiteConfig();
            cfg.setId(1);
            cfg.setConfigJson(defaultConfig());
            return cfg;
        }
    }

    @Override
    public void saveOrUpdate(String configJson) {
        try {
            SiteConfig cfg = new SiteConfig();
            cfg.setId(1);
            cfg.setConfigJson(configJson);
            if (mapper.selectById(1) != null) {
                mapper.updateById(cfg);
            } else {
                mapper.insert(cfg);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("数据库写入失败，请确认 site_config 表已创建");
        }
    }

    private String defaultConfig() {
        return """
        {
          "hero": {
            "tagline": "SINCE 2006 · 乳源本地门窗专家",
            "heading": "高端门窗 匠心定制",
            "desc": "集研发 · 设计 · 生产 · 安装于一体，为品质生活保驾护航"
          },
          "stats": [
            {"num": "20", "suffix": "+", "label": "年行业经验"},
            {"num": "1000", "suffix": "+", "label": "服务家庭"},
            {"num": "98", "suffix": "%", "label": "好评率"},
            {"num": "5", "suffix": "+", "label": "产品系列"}
          ],
          "cta": {
            "heading": "定制您的专属门窗方案",
            "desc": "免费上门测量 · 免费出设计方案 · 免费报价"
          },
          "service": {
            "steps": [
              {"icon": "📏", "title": "免费上门量尺", "desc": "专业师傅携带精密仪器上门，精确测量门窗洞口尺寸，了解需求。", "details": ["全屋门窗尺寸精准测量", "了解采光/通风/隔音需求", "提供初步风格搭配建议"]},
              {"icon": "✏️", "title": "方案设计报价", "desc": "根据测量数据和客户需求，出具设计方案和详细报价清单。", "details": ["推荐合适型材与玻璃配置", "3D 效果图预览（免费）", "透明报价，无隐形消费"]},
              {"icon": "🏭", "title": "生产加工定制", "desc": "工厂按图纸精准加工，严控每一道工序，确保品质达标。", "details": ["高精度数控设备切割组装", "型材表面喷涂/电泳处理", "出厂前全检，合格率 ≥ 99%"]},
              {"icon": "🔧", "title": "专业安装施工", "desc": "经验丰富的安装团队携带专业工具上门，规范安装施工。", "details": ["旧窗拆除 + 垃圾清运", "水平仪校准 → 发泡胶填充 → 密封处理", "完工后现场清扫干净"]},
              {"icon": "✅", "title": "验收交付售后", "desc": "客户现场验收签字确认，建立售后服务档案。", "details": ["开关/密封/锁具逐项验收", "讲解日常使用保养须知", "录入售后系统，享受质保服务"]}
            ],
            "warranty": [
              {"icon": "🛡️", "title": "质保期限", "desc": "型材框架 10年 质保\\n五金配件 3年 质保\\n玻璃密封 2年 质保"},
              {"icon": "🔄", "title": "终身维护", "desc": "质保期内免费维修\\n超期仅收材料成本费\\n定期回访巡检服务"},
              {"icon": "📋", "title": "售后服务", "desc": "售后电话 48小时响应\\n紧急情况 24小时上门\\n维修完成后电话回访"}
            ]
          },
          "about": {
            "heading": "顺居门业",
            "subtitle": "SINCE 2006 · 乳源本地门窗专家",
            "intro": "顺居门业扎根广东省韶关市乳源瑶族自治县，是一家集研发、设计、生产、安装于一体的专业门窗企业。主营断桥隔热门窗、不锈钢防盗门窗、室内定制木门及旧窗改造服务，致力于为本地家庭提供高品质、高性价比的门窗解决方案。\\n公司位于乳源县乳城镇鲜明北路大群奔康住宅楼4号店铺，拥有专业的设计安装团队和标准化服务体系，服务范围覆盖乳源全县及周边地区。",
            "values": [
              {"icon": "🏠", "title": "本地品牌", "desc": "二十载扎根乳源\\n服务千家万户"},
              {"icon": "🛠️", "title": "匠心工艺", "desc": "精选优质型材\\n严苛品控标准"},
              {"icon": "🤝", "title": "诚信服务", "desc": "免费上门测量\\n终身售后维护"}
            ],
            "bizItems": [
              {"icon": "🪟", "text": "断桥隔热门窗"},
              {"icon": "🔒", "text": "不锈钢防盗门窗"},
              {"icon": "🚪", "text": "室内定制木门"},
              {"icon": "🔧", "text": "旧窗改造翻新"},
              {"icon": "🪚", "text": "阳光房定制"},
              {"icon": "📐", "text": "纱窗护栏配件"}
            ],
            "contact": {"address": "广东省韶关市乳源瑶族自治县\\n乳城镇鲜明北路大群奔康住宅楼4号店铺", "phone": "189-4884-6839\\n0751-5368748", "hours": "周一至周日\\n8:00 - 18:00"}
          },
          "faq": [
            {"q": "你们提供免费上门测量吗？", "a": "提供。乳源全县范围内免费上门测量门窗洞口尺寸，并给出初步方案建议，不收取任何费用。"},
            {"q": "定做窗户一般需要多久？", "a": "从测量到安装完成，常规产品约需 7-15 天，具体视产品类型和订单量而定，急单可加急处理。"},
            {"q": "门窗质保多久？", "a": "型材框架质保 10 年，五金配件质保 3 年，玻璃密封质保 2 年。质保期内免费维修更换。"},
            {"q": "旧窗换新需要拆墙吗？", "a": "大多数情况下不需要。我们的旧窗改造采用无损拆换技术，原墙体结构保留，仅更换窗框和玻璃。"},
            {"q": "可以只换玻璃不换窗框吗？", "a": "可以。如果现有窗框结构完好，可单独更换中空玻璃或 Low-E 玻璃，提升隔音隔热效果。"},
            {"q": "断桥铝和普通铝合金有什么区别？", "a": "断桥铝中间有隔热条断开，隔音隔热效果远优于普通铝合金，特别适合乳源山区夏季高温、冬季潮湿的气候。"}
          ],
          "reviews": [
            {"stars": 5, "text": "师傅上门很准时，量尺也专业，装完效果比我想象的好很多，隔音明显提升了", "author": "乳城镇 陈先生", "product": "断桥铝平开窗", "date": "2025.12"},
            {"stars": 5, "text": "旧窗换新做得很好，拆旧装新一天搞定，垃圾都清理干净了，非常省心", "author": "桂头镇 张女士", "product": "旧窗改造", "date": "2025.10"},
            {"stars": 5, "text": "对比了好几家，最后还是选了本地店，价格实在服务也好，售后随叫随到", "author": "大桥镇 李先生", "product": "不锈钢防盗门", "date": "2025.08"}
          ],
          "guide": [
            {"icon": "🚗", "label": "驾车到店", "text": "导航搜索「顺居门业」，G323 国道旁源强门厂西北侧 100 米，门前可停车"},
            {"icon": "🚌", "label": "公交路线", "text": "乘坐乳源公交 1 路/2 路至「鲜明桥」站，步行约 5 分钟即达"},
            {"icon": "🏪", "label": "周边地标", "text": "幸福家园二期对面 · 乳源体育馆往西 300 米"}
          ],
          "series": [
            {"icon": "🪟", "name": "断桥隔热铝合金门窗", "desc": "抗风压 · 隔音隔热，适配乳源山区气候"},
            {"icon": "🔒", "name": "不锈钢防盗门窗", "desc": "高安全防护等级，临街商铺自建房首选"},
            {"icon": "🚪", "name": "室内定制木门", "desc": "实木复合门 · 模压门，定制尺寸与表面处理"},
            {"icon": "🔧", "name": "门窗配件及辅材", "desc": "密封胶条 · 五金锁具 · 滑轨铰链 · 防护栏杆"},
            {"icon": "🏠", "name": "旧窗改造 / 拆换", "desc": "老旧门窗拆换一体化，含墙体修补防水密封"},
            {"icon": "📐", "name": "自建房适配方案", "desc": "乡村自建房抗山林风压门窗系统设计"}
          ],
          "advantages": [
            {"icon": "🏔️", "title": "山区适配", "desc": "针对乳源高湿度山区气候优化，防锈防老化"},
            {"icon": "📏", "title": "免费上门测量", "desc": "乳源全域免费上门，精准出图不误工"},
            {"icon": "🎨", "title": "个性定制", "desc": "尺寸颜色玻璃五金表面处理均可定制"},
            {"icon": "🛠️", "title": "专业安装", "desc": "自有安装团队，拆换一体化，标准化施工"},
            {"icon": "💎", "title": "售后无忧", "desc": "维修耗材包适配，密封胶润滑剂养护支持"},
            {"icon": "🏠", "title": "本地服务", "desc": "乳城镇实体门店，扎根乳源，响应快速"}
          ]
        }
        """;
    }

}

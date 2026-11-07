// SPDX-License-Identifier: Apache-2.0
// Demo tender data and step definitions for the bidding (tender response) page.

export type StepId = 0 | 1 | 2 | 3 | 4 | 5;

export const STEP_LABELS = [
  "招标文件",
  "AI 解读",
  "需求与评分",
  "标书大纲",
  "专业撰写",
  "废标检查",
];

export const TENDER_FILE = "政务服务中心国产化办公终端设备采购项目-招标文件.md";

export const TENDER_TEXT = `# 政务服务中心国产化办公终端设备采购项目招标文件

## 一、项目概况

- 采购人：某市政务服务中心
- 项目类型：货物类
- 采购内容：国产化办公终端、配套操作系统、办公软件及部署服务
- 交付期限：合同签订后 30 个自然日内完成供货、安装、调试和验收
- 质保期限：整机不少于 3 年，提供原厂上门服务

## 二、采购范围

1. 国产化台式办公终端 200 套。
2. 国产化笔记本电脑 50 套。
3. 配套操作系统、流式办公软件、版式办公软件和安全软件授权。
4. 设备部署、数据迁移、人员培训及三年运维服务。

## 三、主要技术参数`;

export const INTERPRET_TEXT = [
  {
    title: "项目概况",
    lines: [
      "采购国产化台式终端 200 套、笔记本 50 套，以及配套软件授权、部署迁移、培训和三年运维服务。",
    ],
  },
  {
    title: "关键时间",
    lines: ["合同签订后 30 个自然日内完成交付、安装、调试和验收", "整机质保不少于 3 年"],
  },
  {
    title: "资格条件",
    lines: [
      "具备独立承担民事责任能力",
      "具有履约所需设备和专业能力",
      "近三年无重大违法记录",
      "不接受联合体投标",
    ],
  },
  {
    title: "交付范围",
    lines: ["终端与软件供货", "业务系统和外设兼容适配", "数据迁移与培训", "三年运维和季度巡检"],
  },
];

export type ReqEntry = {
  id: string;
  title: string;
  required: boolean;
  content: string;
  evidenceLoc: string;
  evidenceText: string;
  score?: string;
  riskLevel?: string;
};

export const RESPONSE_ENTRIES: ReqEntry[] = [
  {
    id: "r1",
    title: "国产化台式终端技术响应",
    required: true,
    content: "台式终端不少于 8 核国产 CPU、16 GB 内存和 512 GB 固态硬盘，显示器不小于 23.8 英寸。",
    evidenceLoc: "第三章 3.1 台式办公终端",
    evidenceText: "处理器应采用国产 CPU，物理核心数不少于 8 核。",
  },
  {
    id: "r2",
    title: "笔记本续航与重量",
    required: true,
    content: "笔记本整机重量不超过 1.6 kg，满电办公续航不低于 8 小时。",
    evidenceLoc: "第三章 3.2 笔记本电脑",
    evidenceText: "笔记本续航时间不低于 8 小时，整机重量不超过 1.6 千克。",
  },
  {
    id: "r3",
    title: "国产操作系统与办公软件适配",
    required: true,
    content: "预装正版国产操作系统，并完成流式与版式办公软件兼容验证。",
    evidenceLoc: "第四章 2.1 软件授权",
    evidenceText: "所有终端须预装经适配认证的国产操作系统。",
  },
  {
    id: "r4",
    title: "故障响应与运维",
    required: true,
    content: "提供 7×24 小时故障响应，重大故障 4 小时内到场处理。",
    evidenceLoc: "第四章 商务与服务要求",
    evidenceText: "故障响应时间不超过 2 小时，重大故障 4 小时内到场。",
  },
];

export const SCORE_ENTRIES: ReqEntry[] = [
  {
    id: "s1",
    title: "技术响应",
    required: true,
    score: "35.00",
    content: "核心参数、兼容适配、稳定性、安全性及可维护性。",
    evidenceLoc: "第六章 6.2 技术响应",
    evidenceText: "核心技术参数全部满足得 20 分。",
  },
  {
    id: "s2",
    title: "实施与服务",
    required: true,
    score: "25.00",
    content: "实施组织、进度保障、培训方案与运维服务承诺。",
    evidenceLoc: "第六章 6.3 实施方案",
    evidenceText: "实施方案完整、进度合理得满分。",
  },
  {
    id: "s3",
    title: "类似业绩",
    required: true,
    score: "10.00",
    content: "近三年同类国产化终端供货与运维业绩。",
    evidenceLoc: "第六章 6.4 业绩要求",
    evidenceText: "每提供一份类似业绩得 2 分，最高 10 分。",
  },
];

export const INVALID_ENTRIES: ReqEntry[] = [
  {
    id: "i1",
    title: "签字盖章不完整",
    required: true,
    riskLevel: "严重",
    content: "投标文件须按要求逐页小签，并在指定位置加盖公章和法人章。",
    evidenceLoc: "第七章 废标条款第 1 项",
    evidenceText: "未按要求签字或加盖公章，投标将被否决。",
  },
  {
    id: "i2",
    title: "核心参数负偏离",
    required: true,
    riskLevel: "严重",
    content: "带★号核心技术参数不允许负偏离，任一负偏离即作废标处理。",
    evidenceLoc: "第七章 废标条款第 2 项",
    evidenceText: "核心技术参数存在负偏离将导致无效投标。",
  },
  {
    id: "i3",
    title: "报价超过最高限价",
    required: true,
    riskLevel: "严重",
    content: "投标总价不得超过采购人设定的最高限价，否则按无效投标处理。",
    evidenceLoc: "第七章 废标条款第 5 项",
    evidenceText: "投标报价超过最高限价的，作无效投标处理。",
  },
];

export type OutlineRow = {
  id: string;
  level: 1 | 2 | 3;
  num: string;
  title: string;
  mapping: string;
};

export const OUTLINE_ROWS: OutlineRow[] = [
  { id: "o1", level: 1, num: "1", title: "资格及商务响应", mapping: "" },
  { id: "o11", level: 2, num: "1.1", title: "资格证明材料", mapping: "" },
  { id: "o111", level: 3, num: "1.1.1", title: "投标函与授权文件", mapping: "废标·签字盖章不完整" },
  { id: "o12", level: 2, num: "1.2", title: "商务条款响应", mapping: "" },
  {
    id: "o121",
    level: 3,
    num: "1.2.1",
    title: "交付、验收与质保承诺",
    mapping: "需求·交付与验收期限",
  },
  { id: "o2", level: 1, num: "2", title: "技术响应方案", mapping: "" },
  { id: "o21", level: 2, num: "2.1", title: "产品技术响应", mapping: "" },
  {
    id: "o211",
    level: 3,
    num: "2.1.1",
    title: "台式终端参数与偏离表",
    mapping: "需求·国产化台式终端技术响应",
  },
  {
    id: "o212",
    level: 3,
    num: "2.1.2",
    title: "笔记本参数与检测证明",
    mapping: "需求·笔记本续航与重量",
  },
  { id: "o22", level: 2, num: "2.2", title: "兼容适配方案", mapping: "" },
  { id: "o221", level: 3, num: "2.2.1", title: "软硬件兼容验证路径", mapping: "" },
];

export type RiskItem = {
  id: string;
  title: string;
  severity: "严重风险" | "高风险" | "中风险";
  evidence: string;
  reason: string;
  suggestion: string;
};

export const RISKS: RiskItem[] = [
  {
    id: "k1",
    title: "签章页仍需人工确认",
    severity: "严重风险",
    evidence: "第七章 废标条款第 1 项 / 资格及商务响应",
    reason: "演示正文中仅保留了签章位置说明，尚无已签章扫描件。",
    suggestion: "导出正式文件后，由项目负责人逐页核对签字盖章位置并留存复核记录。",
  },
  {
    id: "k2",
    title: "核心参数证明材料待补页码",
    severity: "高风险",
    evidence: "第三章 3.1、3.2 / 技术响应方案",
    reason: "生成正文使用的是演示资料，无法自动获知正式检测报告装订页码。",
    suggestion: "装订前补齐 CPU、内存、续航和兼容性证明材料页码，并回填参数偏离表。",
  },
  {
    id: "k3",
    title: "报价与最高限价需复核",
    severity: "中风险",
    evidence: "第七章 废标条款第 5 项",
    reason: "报价属于正式填报数据，演示环境不保存真实商务信息。",
    suggestion: "提交前由商务负责人复算分项报价、税率与总价，并与最高限价比对。",
  },
];

export const RECENT_PROJECTS = [
  {
    name: "政务服务中心国产化办公终端设备采购项目",
    status: "检查完成",
    tag: "货物类",
    updated: "2026-07-28 10:08",
  },
  {
    name: "智慧园区运维服务采购项目（演示）",
    status: "大纲待确认",
    tag: "服务类",
    updated: "2026-07-27 15:46",
  },
];

/* ============================== 小组件 ============================== */

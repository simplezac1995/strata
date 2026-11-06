// SPDX-License-Identifier: Apache-2.0
// Split out of ProofreadingEditor.tsx by scripts/auto-split.mjs.

export interface RuleGroup {
  category: string;
  desc: string;
  items: { name: string; desc: string }[];
}

export const RULE_GROUPS: RuleGroup[] = [
  {
    category: "自定义词库",
    desc: "检测自定义词库中的词汇是否出现。",
    items: [
      { name: "自定义领导人相关", desc: "检测是否存在自定义的领导人信息" },
      { name: "敏感词", desc: "检测是否存在自定义敏感词" },
    ],
  },
  {
    category: "深度纠错",
    desc: "深度纠错专项，包括标点符号、基础质量、政治表述等专项纠错能力。",
    items: [
      { name: "基础质量纠错", desc: "识别错别字及误用词语，规范表达，提升文本质量。" },
      { name: "标点符号纠错", desc: "识别标点使用问题，避免歧义，提升文本准确性。" },
    ],
  },
  {
    category: "基础质量",
    desc: "对稿件中的错别字、语法/句法错误、常识错误和生僻字进行识别和提示。",
    items: [
      { name: "错别字", desc: "识别稿件中的错别字问题。" },
      { name: "语法/句法错误", desc: "识别稿件中的语法和句法错误。" },
      { name: "音/形相似错误", desc: "因汉字的发音或形状相似而导致的错别字。" },
      { name: "标点符号错误", desc: "标点符号使用不正确或位置不当。" },
      { name: "的/地/得 使用错误", desc: "“的、地、得”三者使用不当或混淆。" },
      { name: "数据校验", desc: "测试数据与格式规范校验。" },
    ],
  },
  {
    category: "意识形态",
    desc: "对稿件中存在的意识形态相关内容进行识别和提示。",
    items: [
      { name: "政治表述校对", desc: "规范政治表述与权威口径。" },
      { name: "领导干部相关", desc: "排序、职务、称谓、职称、思想表述等。" },
      { name: "表述错误/表述建议", desc: "常见表述、港澳台主权、民族宗教等。" },
    ],
  },
  {
    category: "安全合规",
    desc: "对稿件中存在的违规负面信息、涉政信息进行识别和提示。",
    items: [
      { name: "合规风险信息", desc: "劣迹艺人、负面人物、敏感信息、暴恐。" },
      { name: "涉政信息", desc: "国外领导人、英雄烈士等。" },
      { name: "机构检测", desc: "机构名称与规范用法检测。" },
    ],
  },
  {
    category: "隐私信息",
    desc: "对稿件中可能出现的个人隐私信息进行识别和提示。",
    items: [
      { name: "电话号码", desc: "识别正文中的电话号码。" },
      { name: "银行卡号", desc: "识别正文中的银行卡号。" },
      { name: "身份证号", desc: "识别正文中的身份证号。" },
      { name: "电子邮件", desc: "识别正文中的电子邮件地址。" },
    ],
  },
];

export const TABS = ["政务性", "通用", "自定义", "疑似"];

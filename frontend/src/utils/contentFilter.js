// ===== 专业敏感词过滤引擎 =====
// 技术：DFA 字典树 + Unicode 全归一化 + 跳符匹配 + 谐音检测 + 拆字还原

// ===== 词库（分类管理，与后端 SensitiveWordFilter.java 同步） =====
const WORD_GROUPS = {
  profanity: [
    // 性相关/下流词汇
    '操','操你','我操','操你妈','操蛋','操比','操逼','操尼玛','操尼妈','被操','求操','想操',
    '草泥马','草拟吗','草拟马','草你妈','草泥马戈壁','草尼玛',
    '肏','肏你','肏尼玛','肏蛋',
    '日你','日尼','日尼玛','日你妈','日你全家','狗日的','狗日',
    '艹','我艹','艹你','艹你妈','艹尼玛',
    '干你','干你妈','干你娘','干尼玛',
    'bitch','b1tch','b!tch','b.i.t.c.h','btch','biatch','biotch',
    'fuck','fuk','fck','fuuck','fucck','f.u.c.k','f u c k','fúck','fück',
    'shit','sh1t','sh!t','sh*t','sht','shite','shít',
    'damn','damm','dam','d4mn','d@mn',
    'dick','d1ck','d!ck','d*k','dck',
    'cock','c0ck','*ck','cok',
    'pussy','p*ssy','puss','puzzy',
    'asshole','assh0le','arsehole',
    'bastard','b*stard','b4stard',
    'whore','wh0re','wh*re',
    'slut','sl*t','sl4t',
    'cunt','c*nt','c4nt',
    'motherfucker','mtherfcker','mofo','mf',
    'wanker','wnker',
    'douche','d0uche',
    'dildo','d1ldo',
    'nigger','n1gger','nigg','n1gg',
    'retard','ret*rd','r3tard',
    // 辱骂/人格侮辱
    '傻逼','傻b','傻B','傻逼玩意','傻逼东西','傻逼玩意儿',
    '煞笔','傻杯','傻叉','傻比','傻批','傻缺','傻屌','傻吊','傻狗','傻X','傻x',
    '弱智','智障','脑残','脑瘫','脑残片','脑子有病','脑子进水','脑子有坑',
    '白痴','蠢货','蠢猪','蠢驴','蠢材','愚笨',
    '废物','垃圾','垃圾人','垃圾玩意','垃圾东西',
    '贱人','贱货','贱B','贱b','贱逼','贱婢','贱种','贱骨头',
    '婊子','婊砸','婊子养的','臭婊子','死婊子',
    '狗逼','狗比','狗币','狗东西','狗娘养的','狗杂种','狗屎',
    '猪猡','猪头','死猪',
    '畜生','禽兽','牲口','杂种','野种',
    '烂人','烂货','烂逼','烂比','烂B',
    '死全家','死一户口本','死妈','死爹','死娘','死爸',
    '龟孙','龟儿子','龟孙子','王八蛋','王八羔子',
    // 下流/色情相关
    '约炮','约p','约P','约pao','打炮','打P','炮友',
    '嫖娼','嫖','嫖客','招嫖','卖淫','卖y','卖Y',
    '包养','求包养','被包养','求包','找金主',
    '大保健','全套','半套','莞式','水疗会所','丝足',
    '看片','簧片','a片','A片','av','AV',
    '裸聊','裸体','裸照','luo聊','luo照','luo体',
    '黄色','黄片','黄网','黄色网站','h网','H网',
    '色情','色q','seqing','seq',
    '激情视频','激情聊天','激情裸聊',
    '一夜情','一夜q','ons','O N S',
    '援交','援助交际','yuanjiao',
    '偷拍','偷窥','偷pai',
    '性交','性爱','性虐','性虐待','性奴',
    '乱伦','乱lun','luanlun',
    '迷奸','迷j','迷jian',
    '做爱','做a','做A','zuoai','zuo爱',
    '射精','射j','shejing',
    '自慰','自w','ziwei','打飞机','打fj',
    '乳交','口交',
    '巨乳','大奶','大波','巨ru',
    '强奸','强j','强jian',
    '轮奸','轮j','轮jian',
    '猥亵','猥xie','weixie',
    '小视频','小v','福利视频','福利姬',
    '萝莉','luoli','loli','幼女','幼齿',
  ],
  pinyin: [
    'cnm','cnmb','cnmdb','cnmlgb','cnmgb',
    'tmd','tmdb','tmlgb',
    'nmsl','nmb','nm$l','nmslwsnd',
    'wqnmlgb','wqnmb','wcnm','wcnmlgb','wqnmlgbd',
    'rnmb','rnm','rnmd','rnmlgb',
    'mlgb','mlgbd','mlgb的',
    'wsnd','wdnmd','nmd',
    'csb','csnm','csndy',
    'sb','s b','s.b','s-b','s_b','$b','sB','Sb',
    '2b','2B','2 b','2-b',
  ],
  spam: [
    // 赌博相关
    '赌博','赌场','赌钱','赌球','赌马','赌狗',
    '博彩','菠菜','bc','B C','B.C',
    '彩票','时时彩','六合彩','快三',
    '百家乐','老虎机','水果机','捕鱼','打鱼','捕鱼游戏',
    '下注','投注','压注','加注','梭哈',
    '菲律宾','缅甸','澳门赌场',
    '棋牌','电玩城','娱乐城','赌城',
    '现金网','信用网','代理线','推广线',
    '真人视讯','真人娱乐','真人荷官','真人发牌',
    // 诈骗/代办/违法
    '代办','代办证件','代办学历','代办签证','代办贷款',
    '刻章','刻公章','办证','办假证','假证',
    '发票','开发票','代开发票','增值税发票',
    '信用卡','办卡','套现','养卡','提额','花呗','白条',
    '贷款','网贷','借贷','放款','小额贷','无抵押','秒到账','黑户',
    '刷单','刷信誉','刷钻','刷信用','刷量','刷粉','刷流量','刷榜',
    '传销','拉人头','发展下线','金字塔','庞氏',
    '资金盘','互助盘','拆分盘','分红盘','原始股',
    '高利贷','高炮','砍头息','714高炮',
    '洗钱','洗黑钱','跑分','跑分平台','代收代付',
    '网赌','网投','网上投注','在线投注',
    // 广告营销骚扰
    '加我qq','加我QQ','加扣扣','扣扣号','qq号',
    '加好友','添加好友','加我好友',
    '微商','代理','招代理','招加盟','诚招代理',
    '减肥','瘦身','丰胸','壮阳','增粗','延时',
    '股票','炒股','荐股','牛股','涨停','内幕消息','老师带单',
    '比特币','虚拟币','数字货币','挖矿','矿机','区块链','token','空投',
    '外汇','黄金','期货','原油','现货',
    '课程','培训','考证','保过','包过',
    '包治','根治','秘方','祖传','特效药','特效',
    // 联系方式引流
    '加微信','加我微信','加v信','加V','加我V','加我v',
    '扫码','扫一扫','扫码加','扫码领取','扫码进群',
    '加群','进群','拉群','入群','qq群','Q群','Q Q群',
    '点击领取','点击查看','点击进入','点击打开','点击链接',
    '兼职','日结','日赚','在家赚钱','手机赚钱','学生兼职','宝妈兼职',
    '薅羊毛','撸羊毛','撸平台','漏洞单','薅毛',
    '中奖','中大奖','恭喜中奖','幸运用户','幸运儿','特等奖',
    '免费领取','免费送','免费拿','免费获取','免费赠送','0元购',
  ],
  political: [
    '反党','反共','反华','反中','反政府',
    '台独','港独','藏独','疆独','东突',
    '法轮功','法轮','falun','Falun',
    '六四','天安门事件','坦克人',
    '颠覆国家','推翻政府','颜色革命',
  ],
}

// 合并为扁平数组
const BAD_RAW = Object.values(WORD_GROUPS).flat()

// ===== Unicode 字符归一化映射表 =====
const UNICODE_NORM = {
  // 全角字母 → 半角
  'ａ':'a','ｂ':'b','ｃ':'c','ｄ':'d','ｅ':'e','ｆ':'f','ｇ':'g','ｈ':'h','ｉ':'i','ｊ':'j','ｋ':'k','ｌ':'l','ｍ':'m',
  'ｎ':'n','ｏ':'o','ｐ':'p','ｑ':'q','ｒ':'r','ｓ':'s','ｔ':'t','ｕ':'u','ｖ':'v','ｗ':'w','ｘ':'x','ｙ':'y','ｚ':'z',
  'Ａ':'a','Ｂ':'b','Ｃ':'c','Ｄ':'d','Ｅ':'e','Ｆ':'f','Ｇ':'g','Ｈ':'h','Ｉ':'i','Ｊ':'j','Ｋ':'k','Ｌ':'l','Ｍ':'m',
  'Ｎ':'n','Ｏ':'o','Ｐ':'p','Ｑ':'q','Ｒ':'r','Ｓ':'s','Ｔ':'t','Ｕ':'u','Ｖ':'v','Ｗ':'w','Ｘ':'x','Ｙ':'y','Ｚ':'z',
  // 全角数字 → 半角
  '０':'0','１':'1','２':'2','３':'3','４':'4','５':'5','６':'6','７':'7','８':'8','９':'9',
  // 圈号数字/字母
  '⓪':'0','①':'1','②':'2','③':'3','④':'4','⑤':'5','⑥':'6','⑦':'7','⑧':'8','⑨':'9',
  'ⓐ':'a','ⓑ':'b','ⓒ':'c','ⓓ':'d','ⓔ':'e','ⓕ':'f','ⓖ':'g','ⓗ':'h','ⓘ':'i','ⓙ':'j','ⓚ':'k','ⓛ':'l','ⓜ':'m',
  'ⓝ':'n','ⓞ':'o','ⓟ':'p','ⓠ':'q','ⓡ':'r','ⓢ':'s','ⓣ':'t','ⓤ':'u','ⓥ':'v','ⓦ':'w','ⓧ':'x','ⓨ':'y','ⓩ':'z',
  // 上标/下标
  '⁰':'0','¹':'1','²':'2','³':'3','⁴':'4','⁵':'5','⁶':'6','⁷':'7','⁸':'8','⁹':'9',
  '₀':'0','₁':'1','₂':'2','₃':'3','₄':'4','₅':'5','₆':'6','₇':'7','₈':'8','₉':'9',
  // 特殊字母变体
  '⒜':'a','⒝':'b','⒞':'c','⒟':'d','⒠':'e','⒡':'f','⒢':'g','⒣':'h','⒤':'i','⒥':'j','⒦':'k','⒧':'l','⒨':'m',
  '⒩':'n','⒪':'o','⒫':'p','⒬':'q','⒭':'r','⒮':'s','⒯':'t','⒰':'u','⒱':'v','⒲':'w','⒳':'x','⒴':'y','⒵':'z',
  // 俄语/希腊混淆字母
  'а':'a','е':'e','о':'o','р':'p','с':'c','у':'y','х':'x','і':'i','ɑ':'a','ο':'o',
  // 符号替换
  '@':'a','₳':'a','ɐ':'a','α':'a','ą':'a','ª':'a',
  'Ɛ':'e','ε':'e','€':'e',
  'Ī':'i','ΐ':'i','ί':'i',
  'Ø':'o','Φ':'o','Θ':'o',
  '₮':'t',
  '$':'s',
  '!':'i','¡':'i',
  '|':'l',
}

// 跳符字符（匹配时自动跳过）
const SKIP_WORDS = new Set(' \r\n\t　.,;:!?()（）【】[]{}《》<>＂\'"`~@#$%^&*-+=_/\\|~·…×'.split(''))

// 繁简映射（覆盖常用繁简字）
const TRAD_SIMP = {
  '幹':'干','媽':'妈','嗎':'吗','們':'们','門':'门','麼':'么','說':'说','來':'来','對':'对',
  '時':'时','會':'会','個':'个','為':'为','國':'国','這':'这','軍':'军','發':'发','現':'现',
  '後':'后','開':'开','關':'关','長':'长','書':'书','兒':'儿','頭':'头','無':'无','體':'体',
  '機':'机','氣':'气','愛':'爱','電':'电','實':'实','學':'学','點':'点','當':'当','過':'过',
}

/** 归一化单个字符 */
function normChar(ch) {
  if (TRAD_SIMP[ch]) return TRAD_SIMP[ch]
  if (UNICODE_NORM[ch]) return UNICODE_NORM[ch]
  return ch
}

/** 归一化整个字符串：去除跳符 + 归一化 + 重复压缩 */
function normalize(text) {
  let result = ''
  let prevNorm = ''
  for (const ch of text) {
    if (SKIP_WORDS.has(ch)) continue
    const nc = normChar(ch).toLowerCase()
    if (nc === prevNorm) continue // 连续重复压缩
    result += nc
    prevNorm = nc
  }
  return result
}

/** 构建包含跳符的"跳转"正则：在词中每两个字之间插入跳符匹配 */
function buildSkipPattern(word) {
  if (word.length < 2) return word
  const skips = '[ 　\\r\\n\\t.,;:!?()（）【】\\[\\]{}《》<>＂\'\"`~@#$%^&*\\-+=_/\\\\|~·…×]{0,2}'
  return word.split('').join(skips)
}

// ===== DFA 字典树 =====
class TrieNode {
  constructor() { this.children = {}; this.isEnd = false; this.word = '' }
}
const trieRoot = new TrieNode()
for (const w of BAD_RAW) {
  let node = trieRoot
  const nw = normalize(w)
  for (const ch of nw) {
    if (!node.children[ch]) node.children[ch] = new TrieNode()
    node = node.children[ch]
  }
  node.isEnd = true; node.word = w
}

/** DFA 在归一化文本中匹配 */
function matchDFA(normalizedText) {
  for (let i = 0; i < normalizedText.length; i++) {
    let node = trieRoot
    for (let j = i; j < normalizedText.length; j++) {
      const ch = normalizedText[j]
      if (!node.children[ch]) break
      node = node.children[ch]
      if (node.isEnd) return node.word
    }
  }
  return null
}

// ===== 谐音/拼音映射 =====
const PINYIN_MAP = {
  'cao': '操', 'kao': '靠', 'ri': '日', 'gan': '干', 'diao': '屌', 'bi': '逼', 'sha': '傻',
  'biao': '婊', 'jian': '贱', 'ma': '妈', 'gou': '狗', 'wang': '王', 'gui': '龟',
}

// ===== 对外接口 =====

/**
 * 检测敏感内容
 * @param {string} text 用户输入文本
 * @returns {string|null} 敏感词类别或 null
 */
export function checkContent(text) {
  if (!text || !text.trim()) return null
  const raw = text.toLowerCase()
  const rawNoSpace = raw.replace(/\s/g, '')

  // 1. URL / 邮箱 / 长数字（原始文本直接匹配）
  if (/https?:\/\/|www\.|\.com|\.cn|\.net|\.top|\.xyz|\.cc|\.vip|\.org|\.io/.test(rawNoSpace)) return '网址链接'
  if (/@\w+\./.test(rawNoSpace)) return '邮箱地址'
  if (/\d{11,}/.test(rawNoSpace.replace(/\D/g, ''))) return '长数字串'

  // 2. 拼音缩写匹配（原始去空格）
  for (const w of WORD_GROUPS.pinyin) { if (rawNoSpace.includes(w)) return '拼音缩写' }

  // 3. 原始文本直接匹配
  for (const w of BAD_RAW) { if (rawNoSpace.includes(w.toLowerCase())) return '敏感词' }

  // 4. 归一化 + DFA 匹配
  const norm = normalize(text)
  if (matchDFA(norm)) return '敏感词'

  // 5. 跳符匹配（防 "操@你 妈" 等绕过）
  for (const w of BAD_RAW) {
    if (w.length < 2) continue
    const pat = buildSkipPattern(w)
    try { if (new RegExp(pat, 'i').test(rawNoSpace)) return '敏感词' } catch {}
  }

  // 6. 谐音检测：提取纯字母串尝试拼音匹配
  const letterOnly = rawNoSpace.replace(/[^a-z]/g, '')
  if (letterOnly.length >= 2) {
    for (const [py, ch] of Object.entries(PINYIN_MAP)) {
      if (letterOnly.includes(py)) {
        // 检查归一化文本是否含对应汉字
        if (norm.includes(ch)) return '敏感词'
      }
    }
  }

  // 7. 拆字检测（"亻尔 女子" → 你 好）
  const splitChars = detectSplitChars(rawNoSpace)
  if (splitChars.length >= 2) {
    for (const w of BAD_RAW) {
      if (w.length < 2) continue
      if (splitChars.join('').includes(w)) return '敏感词'
    }
  }

  return null
}

// 常见拆字偏旁映射
const RADICAL_MAP = { '亻尔': '你', '女子': '好', '日天': '昊', '小女且': '姐', '木子': '李' }
function detectSplitChars(text) {
  const result = []
  for (const [rad, ch] of Object.entries(RADICAL_MAP)) {
    if (text.includes(rad)) result.push(ch)
  }
  return result
}

/** 格式化提示消息 */
export function formatFilterMsg(word) {
  if (!word) return null
  const map = { '网址链接': '请勿在内容中包含网址链接', '邮箱地址': '请勿包含邮箱地址', '长数字串': '请勿包含长数字串', '拼音缩写': '请勿使用不文明用语' }
  return map[word] || '内容包含敏感信息，请文明交流'
}

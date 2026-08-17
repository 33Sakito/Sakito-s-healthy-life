# Sakito 健康生活

一个本地优先的 Android 饮食与身体记录 App，使用 **Kotlin + Jetpack Compose + Room + DataStore + WorkManager** 构建。

## 功能概览

- **食物库**
  - 每 100g 营养数据：热量、总蛋白、动物蛋白、植物蛋白、脂肪、总碳水、膳食纤维、备注
  - 自定义微量营养素标记（不参与汇总）
  - 基础单位“克”不可删除，可添加任意自定义单位（个、碗、勺…）
  - 搜索 / 常用 / 最近使用 / 自定义筛选
  - 组合食物（食谱）：多食材按单位与数量自动计算总营养，支持“一份”和“每100克”，嵌套组合带循环引用检测
  - 内置基础食物库，支持 CSV 导入/导出（UTF-8 with BOM）

- **饮食记录**
  - 日期、餐次（早餐/午餐/晚餐/加餐/夜宵/全天/未分餐）、备注
  - 每条记录可包含多个食物条目，自动计算实际重量与营养贡献
  - 快捷添加常用/最近食物，复制单条记录或整日记录
  - 所有数值保留可配置小数位（默认 1 位）

- **体重与身体维度**
  - 体重记录：日期、体重、备注，同一天可多次
  - 身体围度：腰围、胸围、臀围、大腿围、小腿围、手臂围，支持自定义维度类型
  - 不记录体脂率

- **统计与趋势**
  - 通用折线图：体重、围度、每日营养（热量/蛋白/脂肪/碳水/纤维等）
  - 预设时间范围：最近一周/一月/三月/全部；支持自定义范围并保存多个命名范围
  - 同一天多记录可切换“最新值 / 平均值”
  - 日历视图用不同颜色标记饮食、体重、围度记录天数

- **数据导出与备份**
  - 一键导出 ZIP：`foods.csv`、`diet_records.csv`、`weight_records.csv`、`body_measurements.csv`、`backup.json`
  - 导入 `backup.json` 完整恢复（覆盖当前数据）
  - 所有数据仅存本地，不联网

- **每日记录提醒**
  - 默认开启，默认 20:00
  - 到点若当天无任何记录则发送系统通知
  - 可完全关闭或修改时间

- **设置**
  - 数据管理：导出全部、导入备份
  - 食物库 CSV 导入/导出
  - 提醒开关与时间
  - 小数位数、默认餐次、趋势图默认取值

## 技术栈

| 层 | 选型 |
| --- | --- |
| UI | Jetpack Compose + Material 3 |
| 本地数据库 | Room (SQLite) |
| 设置存储 | DataStore Preferences |
| 后台提醒 | WorkManager + 系统通知 |
| 备份/CSV | java.util.zip + org.json + 手写 CSV 解析 |
| 图表 | Compose Canvas 自绘折线图 |

## 项目结构

```
app/src/main/java/com/sakito/healthylife/
├── data/
│   ├── local/          # Room 实体、DAO、数据库、基础数据种子
│   ├── model/          # UI/业务模型
│   ├── repository/     # 食物、饮食、身体、统计仓库
│   ├── settings/       # DataStore 设置
│   └── backup/         # ZIP/CSV 导入导出与备份恢复
├── notification/       # 每日提醒 Worker 与调度
├── ui/
│   ├── components/     # 通用组件（营养卡片、折线图等）
│   ├── screens/        # 各页面
│   ├── theme/          # Material 3 主题
│   ├── viewmodel/      # ViewModel 与 Factory
│   └── AppRoot.kt      # 导航与底部栏
└── MainActivity.kt
```

## 构建运行

1. 使用 **Android Studio** 打开本项目根目录。
2. 等待 Gradle Sync 完成（要求 AGP 8.5+、JDK 17）。
3. 连接 Android 设备或启动模拟器（minSdk 26 / Android 8.0+）。
4. 点击 Run `app`。

命令行构建（如已安装 JDK 17 与 Android SDK）：

```bash
./gradlew :app:assembleDebug
```

> 首次启动会自动写入 `assets/foods_base.csv` 中的基础食物库和默认围度类型。

## 数据说明

- 数据库文件：`/data/data/com.sakito.healthylife/databases/sakito_healthy_life.db`
- 导出文件通过系统文件选择器保存到用户文档目录
- CSV 均为 UTF-8 with BOM，可直接用 Excel 打开

## 已知简化项

- 组合食物创建后暂未提供“编辑组成食材”的独立界面；可通过删除后重建，或直接编辑营养字段（营养字段在组合食物上仍可手动修改）。
- 提醒的“App 内横幅”目前以系统通知为主；若用户关闭通知权限，前台不会额外弹横幅。
- 统计页营养日均值按当前选择范围的日历天数计算。

// SPDX-License-Identifier: Apache-2.0
import { useMemo, useState, type ReactNode } from "react";
import AddIcon from "@mui/icons-material/Add";
import DeleteIcon from "@mui/icons-material/DeleteOutlined";
import CloudUploadIcon from "@mui/icons-material/CloudUpload";
import SearchIcon from "@mui/icons-material/Search";
import DownloadIcon from "@mui/icons-material/Download";
import RefreshIcon from "@mui/icons-material/Refresh";
import CloseIcon from "@mui/icons-material/Close";
import CheckIcon from "@mui/icons-material/Check";
import ArrowBackIcon from "@mui/icons-material/ArrowBack";
import DnsIcon from "@mui/icons-material/Dns";
import CheckCircleIcon from "@mui/icons-material/CheckCircle";
import EditIcon from "@mui/icons-material/Edit";
import { Avatar, Box, Chip, Drawer, IconButton, Paper, Typography } from "@mui/material";
import { Button, EmptyState, TextField, TextArea } from "@/components/ui/index";
import { PageStack } from "@/shared/pageKit/PageLayout";

interface DbType {
  name: string;
  desc: string;
  emoji: string;
  port: number;
  jdbc: string;
}

const DB_TYPES: DbType[] = [
  {
    name: "PostgreSQL",
    desc: "功能强大的开源对象关系型数据库",
    emoji: "🐘",
    port: 5432,
    jdbc: "postgresql",
  },
];

type TestState = "untested" | "testing" | "ok";

/** Card shape aligned with the future datasource DTO (no list endpoint yet). */
interface DataSourceCard {
  alias: string;
  type: string;
  username: string;
  url: string;
}

export function DataSources() {
  // No datasource list endpoint exists yet → render the real empty state and the
  // create-flow drawer. `rows` stays empty until the backend endpoint lands.
  const [rows] = useState<DataSourceCard[]>([]);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [step, setStep] = useState(0);
  const [dbType, setDbType] = useState<DbType | null>(null);
  const [alias, setAlias] = useState("");
  const [host, setHost] = useState("");
  const [port, setPort] = useState("5432");
  const [dbName, setDbName] = useState("");
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [testState, setTestState] = useState<TestState>("untested");

  const jdbcUrl = useMemo(() => {
    if (!dbType || (!host && !dbName)) return "";
    return `jdbc:${dbType.jdbc}://${host || "localhost"}:${port || dbType.port}/${dbName}`;
  }, [dbType, host, port, dbName]);

  const openCreate = () => {
    setStep(0);
    setDbType(null);
    setAlias("");
    setHost("");
    setPort(String(DB_TYPES[0].port));
    setDbName("");
    setUsername("");
    setPassword("");
    setTestState("untested");
    setDrawerOpen(true);
  };

  const pickType = (t: DbType) => {
    setDbType(t);
    setPort(String(t.port));
  };

  const runTest = () => {
    setTestState("testing");
    window.setTimeout(() => setTestState("ok"), 700);
  };

  return (
    <PageStack>
      {/* Toolbar: left actions + right utility icon buttons (pigcloud style) */}
      <Box
        sx={{
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
          flexWrap: "wrap",
          gap: 1,
        }}
      >
        <Box sx={{ display: "flex", gap: 1 }}>
          <Button variant="contained" startIcon={<AddIcon />} onClick={openCreate}>
            新增
          </Button>
          <Button variant="outlined" color="default" startIcon={<CloudUploadIcon />}>
            导入
          </Button>
          <Button variant="outlined" color="default" startIcon={<DeleteIcon />}>
            删除
          </Button>
        </Box>
        <Box sx={{ display: "flex", gap: 1 }}>
          {[SearchIcon, DownloadIcon, RefreshIcon].map((Icon, i) => (
            <IconButton
              key={i}
              sx={{ border: 1, borderColor: "divider", borderRadius: 1.5, width: 38, height: 38 }}
            >
              <Icon fontSize="small" />
            </IconButton>
          ))}
        </Box>
      </Box>

      {/* Card grid — real empty state until a datasource list endpoint lands */}
      {rows.length === 0 ? (
        <EmptyState icon={<DnsIcon />} message="暂无数据源，点击「新增」接入第一个数据库" />
      ) : (
        <Box
          sx={{
            display: "grid",
            gridTemplateColumns: { xs: "1fr", sm: "repeat(2, 1fr)", lg: "repeat(3, 1fr)" },
            gap: 2,
          }}
        >
          {rows.map((r) => (
            <Paper key={r.alias} variant="outlined" sx={{ p: 2.5, borderRadius: 2 }}>
              <Box sx={{ display: "flex", alignItems: "center", gap: 2 }}>
                <Avatar sx={{ bgcolor: "primary.main" }}>
                  <DnsIcon />
                </Avatar>
                <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
                  {r.alias}
                </Typography>
                <Chip label={r.type} size="small" variant="outlined" />
              </Box>
              <Typography variant="body2" sx={{ color: "text.secondary", mt: 1.5 }}>
                用户名: {r.username}
              </Typography>
              <Typography variant="body2" sx={{ color: "text.secondary" }} noWrap>
                连接地址: {r.url}
              </Typography>
              <Box sx={{ display: "flex", gap: 1, mt: 1.5, justifyContent: "center" }}>
                <Button size="small" variant="text" startIcon={<EditIcon />}>
                  编辑
                </Button>
                <Button size="small" variant="text" color="danger" startIcon={<DeleteIcon />}>
                  删除
                </Button>
              </Box>
            </Paper>
          ))}
        </Box>
      )}

      {/* Right drawer — two-step wizard */}
      <Drawer
        anchor="right"
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        sx={{ "& .MuiDrawer-paper": { width: { xs: "100%", sm: 640 }, p: 0 } }}
      >
        <Box
          sx={{
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            px: 3,
            pt: 2.5,
          }}
        >
          <Typography variant="h6">新建数据源 - {step === 0 ? "选择类型" : "配置连接"}</Typography>
          <IconButton onClick={() => setDrawerOpen(false)}>
            <CloseIcon />
          </IconButton>
        </Box>

        {/* Custom stepper */}
        <Box sx={{ display: "flex", alignItems: "flex-start", px: 6, pt: 3, pb: 1 }}>
          <StepCircle done={step > 0} active={step === 0} label="选择数据源" sub="选择数据源类型" />
          <Box
            sx={{
              flex: 1,
              height: 1,
              bgcolor: step >= 1 ? "primary.main" : "divider",
              mt: 4,
              mx: 2,
            }}
          />
          <StepCircle
            done={false}
            active={step === 1}
            label="配置信息"
            sub="填写连接配置"
            value="2"
          />
        </Box>

        <Box sx={{ px: 3, py: 2, flex: 1, overflowY: "auto" }}>
          {step === 0 ? (
            <Box sx={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 1.5 }}>
              {DB_TYPES.map((t) => {
                const selected = dbType?.name === t.name;
                return (
                  <Paper
                    key={t.name}
                    variant="outlined"
                    onClick={() => pickType(t)}
                    sx={{
                      p: 2,
                      cursor: "pointer",
                      display: "flex",
                      alignItems: "center",
                      gap: 1.5,
                      borderColor: selected ? "primary.main" : "divider",
                      borderWidth: selected ? 2 : 1,
                      "&:hover": { borderColor: "primary.main" },
                    }}
                  >
                    <Typography sx={{ fontSize: 28 }}>{t.emoji}</Typography>
                    <Box>
                      <Typography variant="subtitle2" sx={{ fontWeight: 600 }}>
                        {t.name}
                      </Typography>
                      <Typography variant="caption" sx={{ color: "text.secondary" }}>
                        {t.desc}
                      </Typography>
                    </Box>
                  </Paper>
                );
              })}
            </Box>
          ) : (
            <Box>
              <Box
                sx={{ display: "flex", alignItems: "flex-start", justifyContent: "space-between" }}
              >
                <Box>
                  <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
                    <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
                      {dbType?.name} 连接配置
                    </Typography>
                    <Chip label="表单配置" size="small" variant="outlined" />
                  </Box>
                  <Typography variant="body2" sx={{ color: "text.secondary" }}>
                    填写连接参数后，可先测试连通性再完成配置。
                  </Typography>
                </Box>
                <Chip
                  size="small"
                  variant="outlined"
                  icon={testState === "ok" ? <CheckCircleIcon sx={{ fontSize: 16 }} /> : undefined}
                  label={
                    testState === "ok" ? "连接成功" : testState === "testing" ? "测试中…" : "未测试"
                  }
                  color={testState === "ok" ? "success" : "default"}
                />
              </Box>

              <Box sx={{ mt: 2.5, display: "flex", flexDirection: "column", gap: 2 }}>
                <FormRow label="* 数据源别名">
                  <TextField
                    placeholder="请输入数据源别名，用于标识该数据源"
                    value={alias}
                    onChange={(e) => setAlias(e.target.value)}
                  />
                </FormRow>
                <FormRow label="配置方式">
                  <Button size="small" variant="contained">
                    表单配置
                  </Button>
                  <Button size="small" variant="text" color="default">
                    手动输入
                  </Button>
                </FormRow>
                <FormRow label="主机地址">
                  <TextField
                    placeholder="例如：localhost 或 192.168.1.100"
                    value={host}
                    onChange={(e) => setHost(e.target.value)}
                  />
                  <Typography sx={{ width: 48 }}>端口</Typography>
                  <TextField
                    fullWidth={false}
                    sx={{ width: 120 }}
                    value={port}
                    onChange={(e) => setPort(e.target.value)}
                  />
                </FormRow>
                <FormRow label="数据库名">
                  <TextField
                    placeholder={`请输入${dbType?.name ?? ""}数据库名称`}
                    value={dbName}
                    onChange={(e) => setDbName(e.target.value)}
                  />
                </FormRow>
                <FormRow label="JDBC URL" alignTop>
                  <Box sx={{ flex: 1 }}>
                    <TextArea rows={2} value={jdbcUrl} />
                    <Typography variant="caption" sx={{ color: "text.secondary" }}>
                      URL 根据上方配置自动生成
                    </Typography>
                  </Box>
                </FormRow>
                <FormRow label="* 用户名">
                  <TextField
                    placeholder="请输入数据库用户名"
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                  />
                </FormRow>
                <FormRow label="* 密码">
                  <TextField
                    type="password"
                    placeholder="请输入数据库密码"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                  />
                </FormRow>
              </Box>
            </Box>
          )}
        </Box>

        <Box
          sx={{
            display: "flex",
            gap: 1,
            p: 2.5,
            borderTop: 1,
            borderColor: "divider",
            justifyContent: "space-between",
          }}
        >
          {step === 1 ? (
            <Button
              variant="outlined"
              color="default"
              startIcon={<ArrowBackIcon />}
              onClick={() => setStep(0)}
            >
              上一步
            </Button>
          ) : (
            <Box />
          )}
          <Box sx={{ display: "flex", gap: 1 }}>
            <Button variant="outlined" color="default" onClick={() => setDrawerOpen(false)}>
              取消
            </Button>
            {step === 1 && (
              <Button variant="outlined" onClick={runTest} disabled={testState === "testing"}>
                {testState === "testing" ? "测试中…" : "测试连接"}
              </Button>
            )}
            <Button
              variant="contained"
              disabled={step === 0 && !dbType}
              onClick={() => {
                if (step === 0) setStep(1);
                else setDrawerOpen(false);
              }}
            >
              {step === 0 ? "下一步" : "完成配置"}
            </Button>
          </Box>
        </Box>
      </Drawer>
    </PageStack>
  );
}

function StepCircle({
  done,
  active,
  label,
  sub,
  value = "1",
}: {
  done: boolean;
  active: boolean;
  label: string;
  sub: string;
  value?: string;
}) {
  return (
    <Box sx={{ textAlign: "center" }}>
      <Box
        sx={{
          width: 32,
          height: 32,
          borderRadius: "50%",
          mx: "auto",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          bgcolor: done || active ? "primary.main" : "action.selected",
          color: done || active ? "primary.contrastText" : "text.secondary",
        }}
      >
        {done ? <CheckIcon sx={{ fontSize: 18 }} /> : value}
      </Box>
      <Typography variant="body2" sx={{ mt: 0.5, fontWeight: 600 }}>
        {label}
      </Typography>
      <Typography variant="caption" sx={{ color: "text.secondary" }}>
        {sub}
      </Typography>
    </Box>
  );
}

function FormRow({
  label,
  children,
  alignTop,
}: {
  label: string;
  children: ReactNode;
  alignTop?: boolean;
}) {
  return (
    <Box sx={{ display: "flex", gap: 2, alignItems: alignTop ? "flex-start" : "center" }}>
      <Typography sx={{ width: 96, pt: alignTop ? 1 : 0 }}>{label}</Typography>
      <Box sx={{ display: "flex", gap: 1.5, alignItems: "center", flex: 1 }}>{children}</Box>
    </Box>
  );
}

export default DataSources;

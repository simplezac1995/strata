// SPDX-License-Identifier: Apache-2.0
import { useEffect, useState } from "react";
import { Grid } from "@mui/material";
import ElectricalServicesOutlinedIcon from "@mui/icons-material/ElectricalServicesOutlined";
import PsychologyOutlinedIcon from "@mui/icons-material/PsychologyOutlined";
import EditNoteOutlinedIcon from "@mui/icons-material/EditNoteOutlined";
import SmartToyOutlinedIcon from "@mui/icons-material/SmartToyOutlined";
import { KpiCard } from "@/components/ui/index";
import { AiPageHeader } from "@/components/ui/index";
import { useI18n } from "@/shared/i18n";
import { providerApi, modelApi, promptApi, agentApi } from "@/shared/api/ai";
import { PageStack } from "@/shared/pageKit/PageLayout";

export function AiOverview() {
  const { t } = useI18n();
  const [counts, setCounts] = useState({ providers: 0, models: 0, prompts: 0, agents: 0 });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    Promise.all([providerApi.list(), modelApi.list(), promptApi.list(), agentApi.list()])
      .then(([providers, models, prompts, agents]) => {
        if (!cancelled) {
          setCounts({
            providers: providers.length,
            models: models.length,
            prompts: prompts.length,
            agents: agents.length,
          });
        }
      })
      .catch(() => {
        if (!cancelled) setCounts({ providers: 0, models: 0, prompts: 0, agents: 0 });
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const value = (n: number) => (loading ? "…" : n);

  return (
    <PageStack>
      <AiPageHeader title={t("ai.config.aiOverview")} />
      <Grid container spacing={2}>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          <KpiCard
            title={t("nav.providers")}
            value={value(counts.providers)}
            icon={<ElectricalServicesOutlinedIcon />}
            color="primary"
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          <KpiCard
            title={t("nav.models")}
            value={value(counts.models)}
            icon={<PsychologyOutlinedIcon />}
            color="secondary"
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          <KpiCard
            title={t("nav.prompts")}
            value={value(counts.prompts)}
            icon={<EditNoteOutlinedIcon />}
            color="success"
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          <KpiCard
            title={t("nav.agents")}
            value={value(counts.agents)}
            icon={<SmartToyOutlinedIcon />}
            color="warning"
          />
        </Grid>
      </Grid>
    </PageStack>
  );
}

export default AiOverview;

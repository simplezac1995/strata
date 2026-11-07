// SPDX-License-Identifier: Apache-2.0
import { CodeReviewView } from "./CodeReviewView";
import { useCodeReview } from "./codereview/useCodeReview";

export function CodeReview() {
  const view = useCodeReview();

  return <CodeReviewView view={view} />;
}

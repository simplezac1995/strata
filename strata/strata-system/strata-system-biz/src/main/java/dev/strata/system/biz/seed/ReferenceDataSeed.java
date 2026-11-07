// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.biz.seed;

import java.util.List;

record ReferenceDataSeed(
        List<PostSeed> posts, List<DictTypeSeed> dictTypes, List<DictDataSeed> dictData, List<ParamSeed> params) {

    record PostSeed(String code, String name, int sortOrder) {}

    record DictTypeSeed(String code, String name, int sortOrder) {}

    record DictDataSeed(String typeCode, String value, String label, int sortOrder) {}

    record ParamSeed(String key, String value, String type, String remark) {}
}

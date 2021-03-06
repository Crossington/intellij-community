/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("DEPRECATION")

package com.intellij.stats.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProgressIndicator
import com.intellij.codeInsight.completion.CompletionService
import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.openapi.util.Key
import com.intellij.util.ReflectionUtil

/**
 * @author Vitaliy.Bibaev
 */
object CompletionUtil {
    val COMPLETION_STARTING_TIME_KEY = Key.create<Long>("com.intellij.stats.completion.starting.time")
    val ML_SORTING_CONTRIBUTION_KEY = Key.create<Long>("com.intellij.stats.completion.ml.contribution")

    fun getCurrentCompletionParameters(): CompletionParameters? = getCurrentCompletion()?.parameters

    fun getShownTimestamp(lookup: LookupImpl): Long? {
        if (lookup.isShown) {
            return ReflectionUtil.getField(LookupImpl::class.java, lookup, Long::class.java, "myStampShown")
        }

        return null
    }

    fun getMLTimeContribution(lookup: LookupImpl): Long? {
        return lookup.getUserData(ML_SORTING_CONTRIBUTION_KEY)
    }

    private fun getCurrentCompletion(): CompletionProgressIndicator? =
            CompletionService.getCompletionService().currentCompletion as? CompletionProgressIndicator
}

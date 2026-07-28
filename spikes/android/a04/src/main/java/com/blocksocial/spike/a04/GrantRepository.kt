package com.blocksocial.spike.a04

class GrantRepository(private val storage: GrantStorage) {

    private val grants = mutableMapOf<String, TemporaryAccessGrant>()

    fun loadAndPurge(now: DeviceTime): List<Pair<String, GrantEvaluation>> {
        grants.clear()
        val discarded = mutableListOf<Pair<String, GrantEvaluation>>()
        storage.readAll().forEach { grant ->
            val evaluation = GrantEvaluator.evaluate(grant, now)
            if (evaluation.suppressesBlock) {
                grants[grant.packageName] = grant
            } else {
                storage.remove(grant.packageName)
                discarded += grant.packageName to evaluation
            }
        }
        return discarded
    }

    fun grant(packageName: String, durationMillis: Long, now: DeviceTime): TemporaryAccessGrant {
        val grant = GrantEvaluator.newGrant(packageName, durationMillis, now)
        grants[packageName] = grant
        storage.write(grant)
        return grant
    }

    fun evaluate(packageName: String, now: DeviceTime): GrantEvaluation? {
        val grant = grants[packageName] ?: return null
        val evaluation = GrantEvaluator.evaluate(grant, now)
        if (!evaluation.suppressesBlock) {
            grants.remove(packageName)
            storage.remove(packageName)
        }
        return evaluation
    }

    fun activePackages(): Set<String> = grants.keys.toSet()
}

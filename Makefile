# QuestClassic — convenience Makefile.
#
# The real build tool is Gradle. This file wraps the most common `./gradlew`
# invocations under short targets so `make install` / `make test` do the
# obvious thing. If something is weird, read the actual `app/build.gradle.kts`.

.PHONY: help build debug release install install-debug install-release \
        test lint clean distclean docs serve-docs sign-check uninstall \
        list-devices tag release-tag

GRADLE ?= ./gradlew
ADB    ?= adb
APK_DEBUG   := app/build/outputs/apk/debug/app-debug.apk
APK_RELEASE := app/build/outputs/apk/release/app-release-unsigned.apk

help: ## This help.
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z_-]+:.*?## / {printf "  \033[1m%-20s\033[0m %s\n", $$1, $$2}' $(MAKEFILE_LIST)

build: ## Assemble both debug and release.
	$(GRADLE) :app:assembleDebug :app:assembleRelease

debug: ## Assemble debug APK only.
	$(GRADLE) :app:assembleDebug

release: ## Assemble release APK (unsigned unless env keystore is set).
	$(GRADLE) :app:assembleRelease

install: install-debug ## Install the debug APK on the first attached headset.

install-debug: debug ## Install the debug APK.
	$(ADB) install -r $(APK_DEBUG)

install-release: release ## Install the release APK (may need re-sign first).
	$(ADB) install -r $(APK_RELEASE)

uninstall: ## Uninstall QuestClassic from the headset.
	-$(ADB) uninstall io.github.questclassic.app
	-$(ADB) uninstall io.github.questclassic.app.debug

test: ## Run unit tests.
	$(GRADLE) :app:testDebugUnitTest

lint: ## Run Android lint.
	$(GRADLE) :app:lintDebug

clean: ## Delete Gradle build outputs.
	$(GRADLE) clean

distclean: clean ## Also wipe the Gradle wrapper and caches.
	rm -rf .gradle build **/build

docs: ## Build the mkdocs-material static site in site/
	@command -v mkdocs >/dev/null 2>&1 || { echo "mkdocs not installed: pip install mkdocs-material"; exit 1; }
	mkdocs build

serve-docs: ## Serve docs locally with hot reload at http://127.0.0.1:8000
	@command -v mkdocs >/dev/null 2>&1 || { echo "mkdocs not installed: pip install mkdocs-material"; exit 1; }
	mkdocs serve

sign-check: ## Print the APK's signing cert (useful after a release build).
	@test -f $(APK_RELEASE) && $(GRADLE) :app:signingReport

list-devices: ## Show attached ADB devices.
	$(ADB) devices -l

tag: ## Create a lightweight tag at HEAD: make tag V=v0.X.Y
	@test -n "$(V)" || { echo "pass V=vX.Y.Z"; exit 2; }
	git tag $(V) -m "QuestClassic $(V)"

release-tag: ## Create a tag AND push it, triggering the Release workflow.
	@test -n "$(V)" || { echo "pass V=vX.Y.Z"; exit 2; }
	git tag $(V) -m "QuestClassic $(V)"
	git push origin $(V)

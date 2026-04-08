<script setup lang="ts">
const props = defineProps<{
  currentStep: number;
  maxNavigableStep: number;
  stepItems: ReadonlyArray<{ step: number; label: string }>;
}>();

const emit = defineEmits<{
  (e: 'go-to-step', step: number): void;
}>();

const onClickStep = (step: number) => {
  if (step > props.maxNavigableStep) return;
  emit('go-to-step', step);
};
</script>

<template>
  <section class="card">
    <h3 class="section-title">단계별 생성</h3>
    <div class="stepper">
      <button
        v-for="step in stepItems"
        :key="step.step"
        type="button"
        class="step-chip"
        :class="{ active: currentStep === step.step }"
        :disabled="step.step > maxNavigableStep"
        @click="onClickStep(step.step)"
      >
        {{ step.label }}
      </button>
    </div>
  </section>
</template>


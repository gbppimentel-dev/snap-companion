"""Offline measurement-boundary checks. Never executes ADB."""
import unittest
import measure_capture as m


def capture(at=1000, **changes):
    return dict(schema=2, session='a'*36, sourceEpoch=1, gameEpoch=2, targetHz=2,
                elapsedRealtimeMillis=at, **{k: changes.pop(k, 0) for k in m.COUNTERS}, **changes)


def row(c, session='c'*36):
    return {'capture': c, 'link': {'session': session, 'sourceEpoch': 2}}


class CaptureMetricsTest(unittest.TestCase):
    def test_rates_include_failed_latency_but_are_not_cpu_or_game_fps(self):
        r = m.delta(capture(), capture(3000, frames=4, readings=3, successfulInferenceMillis=600, failedInferenceMillis=200, inferenceFailures=1))
        self.assertEqual(r['sampledFrameHz'], 2)
        self.assertEqual(r['successfulInferenceHz'], 1.5)
        self.assertEqual(r['successfulInferenceWallTimeRatio'], .3)
        self.assertEqual(r['totalInferenceWallTimeRatio'], .4)
        self.assertNotIn('gameFps', r)

    def test_session_source_target_and_counter_changes_cannot_be_aggregated(self):
        for key, value in [('session', 'b'*36), ('sourceEpoch', 2), ('gameEpoch', 3), ('targetHz', 5), ('elapsedRealtimeMillis', 0), ('frames', -1)]:
            before, after = capture(), capture(3000, frames=4)
            after[key] = value
            with self.subTest(key=key): self.assertIsNone(m.delta(before, after))

    def test_missing_or_boolean_counters_are_rejected(self):
        for invalid in [None, True, -1, 1.5]:
            after = capture(3000, frames=4)
            after['readings'] = invalid
            self.assertIsNone(m.delta(capture(), after))

    def test_contiguous_segments_prevent_a_two_five_two_hz_mix(self):
        rows = []
        for i, hz in enumerate([2, 2, 5, 5, 2, 2]):
            c = capture(1000+i*1000, frames=i*2)
            c['targetHz'] = hz
            rows.append(row(c))
        groups = m.segments(rows)
        self.assertEqual([g['configuredHz'] for g in groups], [2, 5, 2])
        self.assertEqual([g['samples'] for g in groups], [2, 2, 2])

    def test_link_restart_splits_a_capture_session(self):
        groups = m.segments([row(capture()), row(capture(2000, frames=2)), row(capture(3000, frames=4), 'd'*36)])
        self.assertEqual([g['samples'] for g in groups], [2, 1])
        self.assertIsNone(groups[-1]['captureDelta'])

    def test_duplicate_unavailable_and_old_schema_dumps_are_not_accepted(self):
        good = 'snap-metrics|{"schema":2,"kind":"link"}'
        self.assertEqual(m.parse(good, 'link')['schema'], 2)
        for raw in [good+'\n'+good, good.replace('2', '1'), good.replace('"kind":"link"', '"kind":"link","available":false')]:
            with self.assertRaises(ValueError): m.parse(raw, 'link')


if __name__ == '__main__': unittest.main()

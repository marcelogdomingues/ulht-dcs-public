// Basic smoke test: verifies the app widget builds.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:verifier_app/main.dart';

void main() {
  testWidgets('App builds without crashing', (WidgetTester tester) async {
    await tester.pumpWidget(const VerifierApp());
    expect(find.byType(MaterialApp), findsOneWidget);
  });
}

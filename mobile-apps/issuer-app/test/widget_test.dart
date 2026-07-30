// Basic smoke test: verifies the app widget builds.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:issuer_app/main.dart';

void main() {
  testWidgets('App builds without crashing', (WidgetTester tester) async {
    await tester.pumpWidget(const IssuerApp());
    expect(find.byType(MaterialApp), findsOneWidget);
  });
}
